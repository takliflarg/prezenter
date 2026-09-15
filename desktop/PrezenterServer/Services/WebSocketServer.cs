using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

namespace PrezenterServer.Services;

/// <summary>
/// .NET ning ichki HttpListener WebSocket qo'llab-quvvatlashiga
/// asoslangan yengil WebSocket serveri. Tashqi kutubxona shart emas -
/// Windows'da HttpListener http.sys ustida ishlaydi va barqaror.
/// </summary>
public sealed class WebSocketServer : IAsyncDisposable
{
    public const int Port = 9091;
    private static readonly string UrlPrefix = $"http://+:{Port}/prezenter/";

    private readonly HttpListener _listener = new();
    private readonly CommandDispatcher _dispatcher;
    private readonly Dictionary<string, WebSocket> _connections = new();
    private readonly object _lock = new();
    private CancellationTokenSource? _cts;

    public event Action<string, string>? DeviceConnected;
    public event Action<string>? DeviceDisconnected;

    public WebSocketServer(CommandDispatcher dispatcher)
    {
        _dispatcher = dispatcher;
        _listener.Prefixes.Add(UrlPrefix);
    }

    public void Start()
    {
        _cts = new CancellationTokenSource();
        _listener.Start();
        _ = AcceptLoopAsync(_cts.Token);
    }

    public void Stop()
    {
        _cts?.Cancel();
        _listener.Stop();
    }

    private async Task AcceptLoopAsync(CancellationToken token)
    {
        while (!token.IsCancellationRequested)
        {
            HttpListenerContext context;
            try
            {
                context = await _listener.GetContextAsync();
            }
            catch (Exception) when (token.IsCancellationRequested)
            {
                return;
            }

            if (!context.Request.IsWebSocketRequest)
            {
                context.Response.StatusCode = 400;
                context.Response.Close();
                continue;
            }

            _ = HandleClientAsync(context, token);
        }
    }

    private async Task HandleClientAsync(HttpListenerContext context, CancellationToken token)
    {
        var wsContext = await context.AcceptWebSocketAsync(subProtocol: null);
        var socket = wsContext.WebSocket;
        var connectionId = Guid.NewGuid().ToString("N");
        var remoteAddress = context.Request.RemoteEndPoint?.Address.ToString() ?? "noma'lum";

        lock (_lock)
        {
            _connections[connectionId] = socket;
        }

        DeviceConnected?.Invoke(connectionId, remoteAddress);

        var buffer = new byte[8 * 1024];
        try
        {
            while (socket.State == WebSocketState.Open && !token.IsCancellationRequested)
            {
                var received = await ReceiveFullMessageAsync(socket, buffer, token);
                if (received is null)
                {
                    break; // Close frame yoki xatolik
                }

                var response = _dispatcher.Handle(connectionId, received);
                if (response is not null)
                {
                    await SendAsync(socket, response, token);
                }
            }
        }
        catch (Exception)
        {
            // Ulanish kutilmaganda uzildi - jim tarzda tozalaymiz.
        }
        finally
        {
            lock (_lock)
            {
                _connections.Remove(connectionId);
            }

            DeviceDisconnected?.Invoke(connectionId);

            if (socket.State != WebSocketState.Closed)
            {
                try
                {
                    await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "bye", CancellationToken.None);
                }
                catch (Exception)
                {
                    // e'tiborsiz qoldiramiz - ulanish allaqachon yopilgan bo'lishi mumkin
                }
            }
        }
    }

    private static async Task<string?> ReceiveFullMessageAsync(WebSocket socket, byte[] buffer, CancellationToken token)
    {
        using var stream = new MemoryStream();
        WebSocketReceiveResult result;
        do
        {
            result = await socket.ReceiveAsync(buffer, token);
            if (result.MessageType == WebSocketMessageType.Close)
            {
                return null;
            }

            stream.Write(buffer, 0, result.Count);
        } while (!result.EndOfMessage);

        return Encoding.UTF8.GetString(stream.ToArray());
    }

    private static async Task SendAsync(WebSocket socket, object payload, CancellationToken token)
    {
        var json = JsonSerializer.Serialize(payload);
        var bytes = Encoding.UTF8.GetBytes(json);
        await socket.SendAsync(bytes, WebSocketMessageType.Text, endOfMessage: true, token);
    }

    /// <summary>Barcha ulangan qurilmalarga status yuborish (masalan, ovoz o'zgarganda).</summary>
    public async Task BroadcastAsync(object payload)
    {
        List<WebSocket> sockets;
        lock (_lock)
        {
            sockets = _connections.Values.ToList();
        }

        foreach (var socket in sockets.Where(s => s.State == WebSocketState.Open))
        {
            try
            {
                await SendAsync(socket, payload, CancellationToken.None);
            }
            catch (Exception)
            {
                // bitta klientga yuborib bo'lmasa, qolganlarga davom etamiz
            }
        }
    }

    public async ValueTask DisposeAsync()
    {
        Stop();
        _listener.Close();
        _cts?.Dispose();
        await Task.CompletedTask;
    }
}

using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text.Json;
using System.Windows;
using System.Windows.Media.Imaging;
using PrezenterServer.Services;
using QRCoder;

namespace PrezenterServer;

public partial class MainWindow : Window
{
    private readonly WebSocketServer _server;
    private readonly PairingManager _pairingManager;
    private readonly VolumeController _volumeController;

    public MainWindow(WebSocketServer server, PairingManager pairingManager, VolumeController volumeController)
    {
        InitializeComponent();

        _server = server;
        _pairingManager = pairingManager;
        _volumeController = volumeController;

        _server.DeviceConnected += OnDeviceConnected;
        _server.DeviceDisconnected += OnDeviceDisconnected;
        _pairingManager.DeviceApproved += OnDeviceApproved;

        RefreshDevicesList();
        GenerateAndShowQrCode();
    }

    private void NewDeviceButton_Click(object sender, RoutedEventArgs e) => GenerateAndShowQrCode();

    private void GenerateAndShowQrCode()
    {
        var token = _pairingManager.GenerateChallengeToken();
        var ip = GetLocalIPv4Address();
        var payload = JsonSerializer.Serialize(new
        {
            ip,
            port = WebSocketServer.Port,
            token
        });

        ConnectionInfoText.Text = $"{ip}:{WebSocketServer.Port}";

        var qrGenerator = new QRCodeGenerator();
        var qrData = qrGenerator.CreateQrCode(payload, QRCodeGenerator.ECCLevel.Q);
        var pngQr = new PngByteQRCode(qrData);
        var bytes = pngQr.GetGraphic(10);

        var bitmap = new BitmapImage();
        using (var stream = new MemoryStream(bytes))
        {
            bitmap.BeginInit();
            bitmap.CacheOption = BitmapCacheOption.OnLoad;
            bitmap.StreamSource = stream;
            bitmap.EndInit();
        }
        bitmap.Freeze();
        QrImage.Source = bitmap;
    }

    private static string GetLocalIPv4Address()
    {
        foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (nic.OperationalStatus != OperationalStatus.Up)
            {
                continue;
            }

            if (nic.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
            {
                continue;
            }

            foreach (var addr in nic.GetIPProperties().UnicastAddresses)
            {
                if (addr.Address.AddressFamily == AddressFamily.InterNetwork)
                {
                    return addr.Address.ToString();
                }
            }
        }

        return IPAddress.Loopback.ToString();
    }

    private void OnDeviceConnected(string connectionId, string remoteAddress) =>
        Dispatcher.Invoke(RefreshDevicesList);

    private void OnDeviceDisconnected(string connectionId)
    {
        _pairingManager.Disconnect(connectionId);
        Dispatcher.Invoke(RefreshDevicesList);
    }

    private void OnDeviceApproved(ApprovedDevice device) => Dispatcher.Invoke(RefreshDevicesList);

    private void RefreshDevicesList()
    {
        DevicesListBox.Items.Clear();
        foreach (var device in _pairingManager.GetApprovedDevices())
        {
            DevicesListBox.Items.Add($"{device.DeviceName}  —  tasdiqlangan: {device.ApprovedAtUtc.ToLocalTime():g}");
        }
    }
}

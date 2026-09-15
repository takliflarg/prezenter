using System.Windows.Threading;
using PrezenterServer.Overlay;

namespace PrezenterServer.Services;

/// <summary>
/// LaserOverlayWindow'ni boshqaradi. WebSocketServer buyruqlarni fon
/// (background) threadda qabul qiladi, shu bois har bir chaqiruv WPF UI
/// dispatcheriga o'tkaziladi.
/// </summary>
public sealed class LaserController
{
    private readonly Dispatcher _uiDispatcher;
    private LaserOverlayWindow? _window;

    public LaserController(Dispatcher uiDispatcher)
    {
        _uiDispatcher = uiDispatcher;
    }

    public void Show()
    {
        _uiDispatcher.Invoke(() =>
        {
            _window ??= new LaserOverlayWindow();
            _window.Show();
        });
    }

    public void Hide()
    {
        _uiDispatcher.Invoke(() => _window?.Hide());
    }

    public void MoveTo(double relativeX, double relativeY)
    {
        _uiDispatcher.Invoke(() =>
        {
            if (_window is null)
            {
                _window = new LaserOverlayWindow();
                _window.Show();
            }

            _window.MoveDot(relativeX, relativeY);
        });
    }
}

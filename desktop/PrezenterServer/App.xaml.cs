using System.Windows;
using Hardcodet.Wpf.TaskbarNotification;
using PrezenterServer.Services;

namespace PrezenterServer;

public partial class App : Application
{
    private WebSocketServer? _server;
    private MdnsAdvertiser? _advertiser;
    private VolumeController? _volumeController;
    private TaskbarIcon? _trayIcon;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        var keySimulator = new KeySimulator();
        _volumeController = new VolumeController();
        var laserController = new LaserController(Dispatcher);
        var pairingManager = new PairingManager();
        var dispatcher = new CommandDispatcher(keySimulator, _volumeController, laserController, pairingManager);

        _server = new WebSocketServer(dispatcher);
        _advertiser = new MdnsAdvertiser();

        _server.Start();
        _advertiser.Start(WebSocketServer.Port);

        _trayIcon = (TaskbarIcon)FindResource("TrayIcon");

        var mainWindow = new MainWindow(_server, pairingManager, _volumeController);
        MainWindow = mainWindow;
        mainWindow.Show();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _trayIcon?.Dispose();
        _advertiser?.Dispose();
        _server?.Stop();
        _volumeController?.Dispose();
        base.OnExit(e);
    }
}

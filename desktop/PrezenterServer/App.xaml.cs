using System;
using System.Windows;
using System.Windows.Threading;
using Hardcodet.Wpf.TaskbarNotification;
using PrezenterServer.Services;

namespace PrezenterServer;

public partial class App : Application
{
    private WebSocketServer? _server;
    private MdnsAdvertiser? _advertiser;
    private VolumeController? _volumeController;
    private TaskbarIcon? _trayIcon;

    public App()
    {
        // Har qanday kutilmagan xato jim qolib, dastur "hech narsa
        // bo'lmagandek" yopilib qolmasligi uchun - foydalanuvchiga
        // tushunarli xabar ko'rsatamiz.
        DispatcherUnhandledException += OnDispatcherUnhandledException;
        AppDomain.CurrentDomain.UnhandledException += OnUnhandledException;
    }

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        try
        {
            var keySimulator = new KeySimulator();
            _volumeController = new VolumeController();
            var laserController = new LaserController(Dispatcher);
            var pairingManager = new PairingManager();
            var dispatcher = new CommandDispatcher(keySimulator, _volumeController, laserController, pairingManager);

            _server = new WebSocketServer(dispatcher);
            _advertiser = new MdnsAdvertiser();

            FirewallHelper.EnsureInboundRuleExists(WebSocketServer.Port);

            _server.Start();

            try
            {
                _advertiser.Start(WebSocketServer.Port);
            }
            catch (Exception ex)
            {
                // mDNS e'lon qilish ba'zi tarmoqlarda (masalan korporativ
                // Wi-Fi) bloklangan bo'lishi mumkin - bu ishning asosiy
                // qismi (WebSocket server) uchun halokatli emas, shuning
                // uchun dasturni to'xtatmasdan davom ettiramiz.
                MessageBox.Show(
                    $"Tarmoqda avtomatik e'lon qilish ishlamadi (QR kod bilan ulanish baribir ishlaydi):\n\n{ex.Message}",
                    "Prezenter - ogohlantirish",
                    MessageBoxButton.OK,
                    MessageBoxImage.Warning);
            }

            _trayIcon = (TaskbarIcon)FindResource("TrayIcon");

            var mainWindow = new MainWindow(_server, pairingManager, _volumeController);
            MainWindow = mainWindow;
            mainWindow.Show();
        }
        catch (Exception ex)
        {
            ShowFatalErrorAndShutdown(ex);
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _trayIcon?.Dispose();
        _advertiser?.Dispose();
        _server?.Stop();
        _volumeController?.Dispose();
        base.OnExit(e);
    }

    private void OnDispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
    {
        ShowFatalErrorAndShutdown(e.Exception);
        e.Handled = true;
    }

    private void OnUnhandledException(object sender, UnhandledExceptionEventArgs e)
    {
        if (e.ExceptionObject is Exception ex)
        {
            ShowFatalErrorAndShutdown(ex);
        }
    }

    private void ShowFatalErrorAndShutdown(Exception ex)
    {
        MessageBox.Show(
            "Prezenter Server ishga tushmadi:\n\n" + ex +
            "\n\nEslatma: dastur \"http://+:9091\" manzilida tinglashi uchun " +
            "administrator huquqi talab qilinadi - dasturni \"Administrator sifatida " +
            "ishga tushirish\" orqali qayta urinib ko'ring.",
            "Prezenter - xato",
            MessageBoxButton.OK,
            MessageBoxImage.Error);

        Shutdown(1);
    }
}

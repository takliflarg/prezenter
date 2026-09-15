using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;

namespace PrezenterServer.Overlay;

/// <summary>
/// Butun virtual ekranni qoplaydigan, sichqoncha/teginishlarni o'zidan
/// "o'tkazib yuboradigan" (click-through) shaffof oyna. Faqat ustida
/// qizil nuqta (lazer) chizadi - prezentatsiyani boshqarishga xalaqit
/// bermaydi, chunki barcha klik/teginishlar pastdagi oynaga tushadi.
/// </summary>
public partial class LaserOverlayWindow : Window
{
    [DllImport("user32.dll")]
    private static extern int GetWindowLong(IntPtr hwnd, int index);

    [DllImport("user32.dll")]
    private static extern int SetWindowLong(IntPtr hwnd, int index, int newStyle);

    private const int GwlExStyle = -20;
    private const int WsExTransparent = 0x00000020;
    private const int WsExLayered = 0x00080000;
    private const int WsExToolWindow = 0x00000080;

    public LaserOverlayWindow()
    {
        InitializeComponent();

        Left = SystemParameters.VirtualScreenLeft;
        Top = SystemParameters.VirtualScreenTop;
        Width = SystemParameters.VirtualScreenWidth;
        Height = SystemParameters.VirtualScreenHeight;
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);

        var hwnd = new WindowInteropHelper(this).Handle;
        var extendedStyle = GetWindowLong(hwnd, GwlExStyle);
        SetWindowLong(hwnd, GwlExStyle, extendedStyle | WsExTransparent | WsExLayered | WsExToolWindow);
    }

    /// <param name="relativeX">0.0-1.0 oralig'ida, ekranning eng chap qirrasidan nisbiy masofa.</param>
    /// <param name="relativeY">0.0-1.0 oralig'ida, ekranning eng yuqori qirrasidan nisbiy masofa.</param>
    public void MoveDot(double relativeX, double relativeY)
    {
        var x = relativeX * Width - LaserDot.Width / 2;
        var y = relativeY * Height - LaserDot.Height / 2;
        Canvas.SetLeft(LaserDot, x);
        Canvas.SetTop(LaserDot, y);
        LaserDot.Visibility = Visibility.Visible;
    }

    public void HideDot() => LaserDot.Visibility = Visibility.Collapsed;
}

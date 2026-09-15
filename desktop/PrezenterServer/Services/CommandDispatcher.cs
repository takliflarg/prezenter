using System.Text.Json;
using PrezenterServer.Models;

namespace PrezenterServer.Services;

/// <summary>
/// Kelgan JSON xabarlarni tegishli boshqaruvchilarga (klaviatura,
/// ovoz, lazer) yo'naltiradi. WebSocketServer har bir ulanishdan
/// olingan xom matnni shu yerga uzatadi.
/// </summary>
public sealed class CommandDispatcher
{
    private readonly KeySimulator _keySimulator;
    private readonly VolumeController _volumeController;
    private readonly LaserController _laserController;
    private readonly PairingManager _pairingManager;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public CommandDispatcher(
        KeySimulator keySimulator,
        VolumeController volumeController,
        LaserController laserController,
        PairingManager pairingManager)
    {
        _keySimulator = keySimulator;
        _volumeController = volumeController;
        _laserController = laserController;
        _pairingManager = pairingManager;
    }

    /// <param name="connectionId">WebSocketServer tomonidan har bir ulanishga beriladigan ID.</param>
    /// <param name="rawJson">Kliyentdan kelgan xom JSON matn.</param>
    /// <returns>Klientga qaytariladigan javob xabari (bo'lmasa null).</returns>
    public object? Handle(string connectionId, string rawJson)
    {
        InboundMessage? message;
        try
        {
            message = JsonSerializer.Deserialize<InboundMessage>(rawJson, JsonOptions);
        }
        catch (JsonException)
        {
            return ErrorMessage.Of("Noto'g'ri JSON formati");
        }

        if (message is null)
        {
            return ErrorMessage.Of("Bo'sh xabar");
        }

        // Pairing bo'lmasa, ulanish tasdiqlanmagan har qanday buyruqni rad etamiz.
        if (message.Type != "pair" && !_pairingManager.IsPaired(connectionId))
        {
            return ErrorMessage.Of("Ulanish hali tasdiqlanmagan (pairing kerak)");
        }

        switch (message.Type)
        {
            case "pair":
                return HandlePair(connectionId, message);

            case "ping":
                return new { type = "pong" };

            case "command":
                return HandleCommand(message.Action);

            case "laser":
                return HandleLaser(message);

            default:
                return ErrorMessage.Of($"Noma'lum xabar turi: {message.Type}");
        }
    }

    private object HandlePair(string connectionId, InboundMessage message)
    {
        var ok = _pairingManager.TryPair(connectionId, message.Token, message.DeviceName ?? "Noma'lum qurilma");
        return ok
            ? PairResultMessage.Ok("Ulanish tasdiqlandi")
            : PairResultMessage.Fail("Pairing tokeni noto'g'ri yoki muddati tugagan");
    }

    private object? HandleCommand(string? action)
    {
        switch (action)
        {
            case CommandAction.Next:
                _keySimulator.SendNextSlide();
                break;
            case CommandAction.Prev:
                _keySimulator.SendPreviousSlide();
                break;
            case CommandAction.StartFromBeginning:
                _keySimulator.SendStartFromBeginning();
                break;
            case CommandAction.StartFromCurrent:
                _keySimulator.SendStartFromCurrentSlide();
                break;
            case CommandAction.Stop:
                _keySimulator.SendStopPresentation();
                break;
            case CommandAction.VolumeUp:
                _volumeController.StepVolume(+1);
                break;
            case CommandAction.VolumeDown:
                _volumeController.StepVolume(-1);
                break;
            case CommandAction.MuteToggle:
                _volumeController.ToggleMute();
                break;
            default:
                return ErrorMessage.Of($"Noma'lum buyruq: {action}");
        }

        return StatusMessage.Create(true, _volumeController.IsMuted, _volumeController.VolumePercent);
    }

    private object? HandleLaser(InboundMessage message)
    {
        switch (message.Action)
        {
            case LaserAction.On:
                _laserController.Show();
                break;
            case LaserAction.Off:
                _laserController.Hide();
                break;
            case LaserAction.Move when message.X is { } x && message.Y is { } y:
                _laserController.MoveTo(x, y);
                break;
            default:
                return ErrorMessage.Of("Noto'g'ri lazer buyrug'i");
        }

        return null;
    }
}

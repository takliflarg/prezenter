using System;
using System.Text.Json.Serialization;

namespace PrezenterServer.Models;

/// <summary>
/// Umumiy inbound xabar konverti. "type" maydoniga qarab
/// CommandDispatcher tegishli qismlarni o'qiydi (boshqa maydonlar
/// type'ga bog'liq holda ixtiyoriy bo'lishi mumkin).
/// </summary>
public sealed class InboundMessage
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    [JsonPropertyName("action")]
    public string? Action { get; set; }

    [JsonPropertyName("token")]
    public string? Token { get; set; }

    [JsonPropertyName("deviceName")]
    public string? DeviceName { get; set; }

    [JsonPropertyName("x")]
    public double? X { get; set; }

    [JsonPropertyName("y")]
    public double? Y { get; set; }
}

public static class CommandAction
{
    public const string Next = "NEXT";
    public const string Prev = "PREV";
    public const string StartFromBeginning = "START_FROM_BEGINNING";
    public const string StartFromCurrent = "START_FROM_CURRENT";
    public const string Stop = "STOP";
    public const string VolumeUp = "VOLUME_UP";
    public const string VolumeDown = "VOLUME_DOWN";
    public const string MuteToggle = "MUTE_TOGGLE";
}

public static class LaserAction
{
    public const string On = "ON";
    public const string Off = "OFF";
    public const string Move = "MOVE";
}

public sealed record StatusMessage(
    [property: JsonPropertyName("type")] string Type,
    [property: JsonPropertyName("connected")] bool Connected,
    [property: JsonPropertyName("computerName")] string ComputerName,
    [property: JsonPropertyName("muted")] bool Muted,
    [property: JsonPropertyName("volume")] int Volume)
{
    public static StatusMessage Create(bool connected, bool muted, int volume) =>
        new("status", connected, Environment.MachineName, muted, volume);
}

public sealed record PairResultMessage(
    [property: JsonPropertyName("type")] string Type,
    [property: JsonPropertyName("success")] bool Success,
    [property: JsonPropertyName("message")] string Message,
    [property: JsonPropertyName("token")] string? Token = null)
{
    public static PairResultMessage Ok(string message, string? token = null) => new("pairResult", true, message, token);
    public static PairResultMessage Fail(string message) => new("pairResult", false, message);
}

public sealed record ErrorMessage(
    [property: JsonPropertyName("type")] string Type,
    [property: JsonPropertyName("message")] string Message)
{
    public static ErrorMessage Of(string message) => new("error", message);
}

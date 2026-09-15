using System;
using System.Diagnostics;

namespace PrezenterServer.Services;

/// <summary>
/// Dastur birinchi marta ishga tushganda Windows Firewall'da WebSocket
/// porti (9091) uchun kiruvchi (inbound) ruxsat qoidasini avtomatik
/// yaratadi. Aks holda telefon tarmoqda kompyuterni topsa-da (mDNS UDP
/// odatda ruxsat etilgan bo'ladi), aynan WebSocket TCP ulanishi Firewall
/// tomonidan jimgina bloklanib qolishi mumkin - foydalanuvchi buni qo'lda
/// sozlashi shart bo'lmasin uchun shu yordamchi kerak (dastur endi admin
/// huquqi bilan ishga tushgani uchun bu buyruq muammosiz bajariladi).
/// </summary>
public static class FirewallHelper
{
    private const string RuleName = "Prezenter Server (TCP 9091)";

    public static void EnsureInboundRuleExists(int port)
    {
        try
        {
            if (RuleExists())
            {
                return;
            }

            RunNetsh($"advfirewall firewall add rule name=\"{RuleName}\" dir=in action=allow protocol=TCP localport={port}");
        }
        catch (Exception)
        {
            // Firewall qoidasini qo'sha olmasak ham, dastur ishlashda
            // davom etadi - foydalanuvchi kerak bo'lsa qo'lda ruxsat beradi.
        }
    }

    private static bool RuleExists()
    {
        var output = RunNetsh($"advfirewall firewall show rule name=\"{RuleName}\"");
        return output.Contains(RuleName, StringComparison.OrdinalIgnoreCase);
    }

    private static string RunNetsh(string arguments)
    {
        var startInfo = new ProcessStartInfo
        {
            FileName = "netsh",
            Arguments = arguments,
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            CreateNoWindow = true
        };

        using var process = Process.Start(startInfo);
        if (process is null)
        {
            return string.Empty;
        }

        var output = process.StandardOutput.ReadToEnd();
        process.WaitForExit(5000);
        return output;
    }
}

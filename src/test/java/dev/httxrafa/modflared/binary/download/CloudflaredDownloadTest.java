package dev.httxrafa.modflared.binary.download;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CloudflaredDownloadTest {

    @Test
    public void findsWindowsAmd64Binary() {
        CloudflaredDownload download = CloudflaredDownload.find("windows 10", "amd64");

        assertEquals("cloudflared-windows-amd64.exe", download.fileName());
        assertEquals("cloudflared-windows-amd64.exe", download.downloadFile());
    }

    @Test
    public void findsLinuxAmd64Binary() {
        CloudflaredDownload download = CloudflaredDownload.find("linux", "amd64");

        assertEquals("cloudflared-linux-amd64", download.fileName());
        assertEquals("cloudflared-linux-amd64", download.downloadFile());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsUnsupportedPlatform() {
        CloudflaredDownload.find("sunos", "sparc");
    }
}

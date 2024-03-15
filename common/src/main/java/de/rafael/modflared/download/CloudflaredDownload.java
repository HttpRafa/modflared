package de.rafael.modflared.download;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum CloudflaredDownload {

    WINDOW_32("windows", "x86", "cloudflared-windows-386.exe", "cloudflared-windows-386.exe"),
    WINDOW_64("windows", "amd64", "cloudflared-windows-amd64.exe", "cloudflared-windows-amd64.exe"),
    LINUX_32("linux", "x86", "cloudflared-linux-386", "cloudflared-linux-386"),
    LINUX_64("linux", "amd64", "cloudflared-linux-amd64", "cloudflared-linux-amd64"),
    MAC_OS_X_64("mac os x", "x86_64", "cloudflared-darwin-amd64", "cloudflared-darwin-amd64.tgz"),

    MAC_OS_ARM("mac os x", "aarch64", "cloudflared-darwin-amd64", "cloudflared-darwin-amd64.tgz");

    private final String osName;
    private final String arch;
    private final String fileName;
    private final String downloadFile;

    CloudflaredDownload(String osName, String arch, String fileName, String downloadFile) {
        this.osName = osName;
        this.arch = arch;
        this.fileName = fileName;
        this.downloadFile = downloadFile;
    }

    public static @NotNull CloudflaredDownload find() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        Optional<CloudflaredDownload> download = Arrays.stream(CloudflaredDownload.values()).filter(item -> osName.contains(item.osName) && arch.contains(item.arch)).findFirst();
        if(download.isPresent()) {
            return download.get();
        } else {
            throw new IllegalStateException("Cloudflared could not be downloaded because no binary file was found for the current operating system");
        }
    }

    public String osName() {
        return osName;
    }

    public String arch() {
        return arch;
    }

    public String fileName() {
        return fileName;
    }

    public String downloadFile() {
        return downloadFile;
    }

}

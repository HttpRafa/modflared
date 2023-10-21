# Modflared
Automatically connects you to a [Cloudflare tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/) without having to install [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/) separately.
#### This mod has not yet been fully tested!

## How to use
To be able to use the mod you have to be on the operating system Windows or Linux, MacOS is not supported. To give the mod information about the tunnel, you have to create a folder called "modflared" in the Minecraft folder. A file called "access.json" must then be created in this folder.

## Example configuration (access.json)
```JSON
[
  {
    "use": true,
    "protocol": "tcp",
    "hostname": "example.domain.net",
    "bind": {
      "host": "127.0.0.1",
      "port": 25565
    }
  }
]
```
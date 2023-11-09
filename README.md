# Modflared
Automatically connects you to a [Cloudflare tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/) without having to install [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/) separately.
#### This mod has not yet been fully tested!

## How to use
To be able to use the mod you have to be on the operating system Windows, Linux, or MacOS. To give the mod information about the tunnel, you have to create a folder called "modflared" in the Minecraft folder. A file called "access.json" must then be created in this folder.

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

## Setting up Cloudflare
You need to set up cloudflare on your server for this to work. There's plenty of guides on how to do this elsewhere.  
Make sure in your config file (possibly in `/etc/cloudflared/config.yml`) you have the lines:  
```YML
- hostname: example.domain.net
  service: tcp://localhost:25565
```
Replace `example.domain.net` with the correct subdomain you want to use. If you're running multiple instances (eg. with docker), change the port 25565 to whatever port you're using.  
Restart the cloudflare daemon (`sudo systemctl restart cloudflared`) to apply the changes.
Add the correct DNS entry: go to [Cloudflare dashboard](https://dash.cloudflare.net), go to your website and DNS entries, then add a new CNAME DNS entry with your subdomain and set the target to `<tunnelID>.cfargotunnel.com`, with the tunnel ID found in the cloudflare config.yml file.

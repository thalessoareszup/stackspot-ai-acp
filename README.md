# StackSpot AI ACP Server

PoC for an ACP server (see https://agentclientprotocol.com/) based on Stackspot AI.

I vendored the kotlin-sdk for ACP under `kotlin-sdk/` because I needed to implement a fix. When/if the fix lands on main we can remove it.

## Usage

Run `./gradlew build install` to install a `acp-server` binary under `$HOME/.local/bin`. 

Assuming this is in your path, set the env vars `STK_ACPSERVER_CLIENT_ID` and `STK_ACPSERVER_CLIENT_SECRET` then run `acp-server -m stdio`.


Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

Speech bubble translation
=========================

Requiem's optional Say It English compatibility adds in-world speech bubbles. When a player is
put into Requiem speech mode, their messages are translated separately for nearby viewers using
each viewer's selected Minecraft language. The translation is performed on the server and does
not require viewers to install a separate translator mod.

Translation uses the following settings in `config/requiem.json` (created when the mod starts):

```json
{
  "translateBubbles": true,
  "translationProvider": "google",
  "libreTranslateUrl": "http://127.0.0.1:5000",
  "libreTranslateApiKey": "",
  "defaultLanguage": "en",
  "timeoutSeconds": 8
}
```

- `google` is the default keyless provider. It uses Google's public web translation endpoint,
  which is undocumented and may be throttled or changed without notice.
- `libretranslate` sends requests to the configured LibreTranslate server. Self-hosted
  instances can be used without a key; hosted instances may require `libreTranslateApiKey`.
- `translateBubbles: false` disables translation and leaves the original text in the bubble.
- `defaultLanguage` is used until a client reports its Minecraft language.
- `timeoutSeconds` is clamped to 2–15 seconds. Translations happen asynchronously, and a
  failed/empty response falls back to the original message.

Admins can also change the live settings in-game:

- `/requiem sayit translation enable`
- `/requiem sayit translation disable`
- `/requiem sayit translation provider google`
- `/requiem sayit translation provider libretranslate`

The LibreTranslate URL and optional API key remain in `config/requiem.json`.

**Privacy:** when translation is enabled, Requiem speech-mode messages are sent from the game
server to the selected external translation provider. Use a private LibreTranslate instance or
set `translateBubbles` to `false` when messages must not leave your server.

# Adjustable S-Mods

Purchase additional S-Mod slots for your ships and ship modules in **Starsector 0.98.5a**.

**Original creator and main author: [Audax](https://github.com/AudaxLudos/adjustable-s-mods).** This is a maintained version of Audax's Adjustable S-Mods, maintained by **saraangelmurphy**.

[![Install with TriOS](https://img.shields.io/badge/Install_with-TriOS-6750A4?style=for-the-badge)](https://trilink.wispborne.com/open.html?mod=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2Fsaraangelmurphy%2Fadjustable-s-mods%2Frefs%2Fheads%2Fmain%2FAdjustableSMods.version%22%2C%22id%22%3A%22adjustablesmods%22%7D&dep=%7B%22id%22%3A%22lw_lazylib%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FLazyWizard%2Flazylib%2Freleases%2Fdownload%2F3.0%2FLazyLib.3.0.zip%22%2C%22version%22%3A%223.0.0%22%7D&dep=%7B%22id%22%3A%22lunalib%22%2C%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FLukas22041%2FLunaLib%2Fmain%2FLunaLib.version%22%2C%22version%22%3A%222.1.0%22%7D)

## Features

- Permanently increase a ship's S-Mod limit, one slot at a time.
- Purchase upgrades for **2, 4, 8, 16... story points**, with the price doubling for each additional slot on that ship.
- Install S-Mods in the extra slots through the game's refit controls, at the usual story-point cost.
- Keep purchased slots with the ship when saving and loading.
- Manage upgrades through a compact panel that uses native game widgets.

## Installation

Requires **LunaLib 2.1.0 or newer** and its **LazyLib** dependency. Supports **Java 17**.

1. Use **Install with TriOS** above, or download **AdjustableSMods.zip** from [Releases](https://github.com/saraangelmurphy/adjustable-s-mods/releases).
2. Import the ZIP into TriOS, or extract it into your Starsector `mods` directory. The installed folder should contain `mods/AdjustableSMods/mod_info.json`.
3. Enable **Adjustable S Mods**, **LunaLib**, and **LazyLib**.

Enable one copy of the mod at a time. Version Checker provides update notifications, direct release downloads and the [changelog](CHANGELOG.txt).

## Usage

Open a ship's refit screen, choose LunaLib's **Additional Options**, then select **Upgrade S-Mod Limit**. The panel shows the ship's purchased slots, the next upgrade's price, and your available story points. Select **Increase Limit** to buy another slot.

Installing an S-Mod in a purchased slot uses the normal game rules.

The original mod's S-Mod removal feature has been removed from this version. To remove or replace existing S-Mod selections, use Starsector 0.98.5a's vanilla colony refit controls at a player-owned colony with Heavy Industry. The game's credit costs and ship restrictions apply.

Keep Adjustable S-Mods enabled when playing saves containing upgraded ships.

## Authors and license

- **Audax:** original creator and main author.
- **saraangelmurphy:** maintainer of this version.

Licensed under [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International](LICENSE.txt).

Additional contributions and acknowledgements are listed in [CREDITS.txt](CREDITS.txt).

<details>
<summary>AI use disclaimer</summary>

Some code and documentation changes in this maintained version were created using AI tools:

- **Code and documentation:** OpenAI Codex, used for the Starsector 0.98.5a compatibility work, bug fixes, refit UI changes, and documentation.

This disclosure covers the maintenance work in this version; credit for the original mod remains with Audax and the contributors listed above.

</details>

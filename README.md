# Did I Miss Something?
 
The project contained in this repository automatically downloads and updates ~mods and configs~ **everything from the modpack repository** of your minecraft client according to a child repository.

It uses [GitHub Api](https://api.github.com/) to get the **latest release** and populates the game with it.

# What loader does this mod support?

DidIMissSomething does not use anything from the minecraft's code, and sooo **any!** Any loader is supported if you can hook into it's loading cycle.

As of now this project is **Architectury-based** and **1.21.1** with support for **fabric** and **neoforge** implemented (I've tried to implement quilt and forge, yet had no time to do so properly), buuut I've left the controls for the build in the main **gradle.settings** so you can ask to add a new branch for the needed version, fork the project, contribute, etc. Whatever you want actually!

# Can I use it with my own repository?

Yes you can! The mod has its own config file (didimisssomething.txt), just specify your **apiURL** and **githubToken** in there and you're done!
- **apiURL** is your github repository address but instead of **https://github.com/author/repositoryname** it is **https://api.github.com/repos/author/repositoryname/releases/latest**
- **githubToken** is needed if your repository is private and you wanna give access to it only through the config file for your modpack, create one [here](https://github.com/settings/tokens) (you can use both Fine-grained and Classic)
- (if you're using gitlab, the api address is **https://gitlab.com/api/v4/projects/projectid/releases**), the token is [here](https://gitlab.com/-/user_settings/personal_access_tokens)

# How to update my mods?

Ohhhh that's simple.

All you need to do is to make a **new release** and make sure to mark it as the **latest release**.

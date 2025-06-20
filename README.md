# Did I Miss Something?
 
The project contained in this repository **automatically downloads and updates everything** on your minecraft client according to a **host repository**.

It uses [GitHub Api](https://api.github.com/) to get the **latest release** and populates the game with it.

# Can I use it with my own repository?

Yes you can! The mod has its own config file (didimisssomething.txt), just specify your **apiURL** and **githubToken** in there and you're done!
- **apiURL** or **mirrorApiURL** is your github repository address, but instead of **https://github.com/author/repositoryname** it is **https://api.github.com/repos/author/repositoryname/releases/latest**
- **githubToken** is needed if your repository is private and you wanna give access to it only through the config file for your modpack, create one [here](https://github.com/settings/tokens) (you can use both Fine-grained and Classic)
- (if you're using gitlab, the api address is **https://gitlab.com/api/v4/projects/projectid/releases**), the token is [here](https://gitlab.com/-/user_settings/personal_access_tokens)

*(Currently only supports github and gitlab URLs, but it's easy to add support to any other repository or archive holder site)*

# How to update my mods?

Ohhhh that's simple, *if doing this for the first time* just **create a new repository** (host) and structure it like your minecraft folder, so put the **\mods** in there, **\config**, and so on.
![image](https://github.com/user-attachments/assets/a858cc95-9c88-4f7a-b884-8dbe912129a3)

Then go in the configs of your client with the mod installed. Start the client once if **didimisssomething.txt** and **didimisssomething-mirror.txt** do not exist in that folder. After that put the correct data into them (**Can I use it with my own repository?** part).

**If you already have an established repository**, all you need to do now is to make a **new release** and make sure to mark it as the **latest release**.

# What loader does this mod support?

**DidIMissSomething does not use anything from the minecraft's code, and sooo any!** Any loader is supported if you can hook into it's loading cycle.

As of now this project is **Architectury-based** and **1.21.1** with support for **fabric** and **neoforge** implemented (I've tried to implement quilt and forge, yet had no time to do so properly).

However I've left the controls for the build in the main **gradle.properties** so you can ask to add a new branch for the needed version, fork the project, contribute, etc. Whatever you want actually!

![image](https://github.com/user-attachments/assets/d5e58537-6f47-4bdf-a857-6257f506d8aa)

# Autoloader?

There is another subproject contained within DidIMissSomething called **Autoloader**. That is the program that runs and downloads everything necessary when there's a new modpack version on the **host repository**.

*IT IS ALSO CURRENTLY NOT IMPLEMENTED AS A PART OF THE BUILD CYCLE!* I mean not *properly*. At the time I've made that I made it into a separate project so merging those two together is needed.

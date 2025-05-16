# Did I Miss Something?
 
The project contained in this repository automatically downloads and updates **mods and configs** of your minecraft client according to a child repository.

It uses [GitHub Api](https://api.github.com/) to get the **latest release** and populates the game with it.

# Can I use it with my own repository?

Yes you can! The mod has its own config file (didimisssomething.txt), just specify your **apiURL** and **githubToken** in there and you're done!
- **apiURL** is your github repository address but instead of **https://github.com/author/repositoryname** it is **https://api.github.com/repos/author/repositoryname/**
- **githubToken** is needed if your repository is private and you wanna give access to it only through the config file for your modpack, create one [here](https://github.com/settings/tokens)
- (if you're using gitlab, the api address is **https://gitlab.com/api/v4/projects/projectid/releases**), the token is [here](https://gitlab.com/-/user_settings/personal_access_tokens)

# How to update my mods?

Ohhhh that's simple.

All you need to do is to make a **new release** and make sure to mark it as the **latest release**.

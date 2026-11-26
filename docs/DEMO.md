# Demo: show the app without any cable or command

One-time setup (once per PC):
1. Double-click `scripts\open-firewall.bat` -> right-click > Run as administrator.
2. Install the app on the phone (cable once, Run from Android Studio, or send the APK).

Each time you demo:
1. Open Docker Desktop and wait for "Engine running".
2. Double-click `scripts\start-demo.bat`. It prints the PC addresses. Keep the window open.
3. Phone on the same Wi-Fi as the PC (or the PC connected to the phone's hotspot).
4. In the app (login screen) tap the "serveur" link at the bottom, type the address (e.g. 192.168.1.20), Save.
5. Log in and play.

Admin account: register in the app, then in pgAdmin run
`UPDATE "Utilisateur" SET "Role"='admin' WHERE "Email"='your@email';` and log in again.

# Manual: sign in to your account

This manual shows how to sign in to PortWallet with the e-mail and the password of an activated account, and how the system keeps you signed in. It describes the behavior in the [FR-02 spec](spec.md) from the point of view of someone using the system.

Buttons, fields and messages are quoted here in Brazilian Portuguese, exactly as they appear on screen, followed by a translation. To change the language, use the **Idioma** (Language) selector at the bottom of any page.

> **Demo environment.** PortWallet moves no real money. You need an account that is already activated: if you do not have one, follow the [registration manual](../fr-01-registration/manual.md) first.

## Before you start

With the project running through Docker (`docker compose up --build`), the system is at <http://localhost:7000>.

| What | Where |
| --- | --- |
| Home screen | <http://localhost:7000> |
| Login | <http://localhost:7000/login> |

## Signing in

1. On the home screen, click **Entrar** ("Sign in"). The **Ir para o login** ("Go to login") button shown right after activating the account leads to the same screen.
2. Fill in the fields:

   | Field | Rule |
   | --- | --- |
   | E-mail | The e-mail of the account. Upper case and surrounding spaces are fixed automatically |
   | Senha (password) | The password you created when activating the account |

3. Click **Entrar**. While the request is on its way the button shows **Entrando...** ("Signing in...") and cannot be clicked again.

When it works, the home screen opens with **"Olá, {your name}"** ("Hello, {your name}") and your name in the top bar. Your balance and transactions will appear on this screen in a later version.

If you were sent to the login screen while trying to open a page that requires an account, you go back to that page after signing in.

## Staying signed in

- **Reloading the page or opening the system again keeps you signed in** for up to **7 days** after the login. Each time you return, the session is renewed in the background; no password is asked.
- After 7 days the session ends, however often you used it, and you need to sign in again.
- Opening the login screen while already signed in takes you straight to the home screen.

> **Why the system is safe even if a page script misbehaves.** The proof of your session is kept in a cookie the browser hides from the page itself, and it is replaced by a new one every time it is used. The short-lived key that the screens use stays only in memory and is lost when the tab closes.

## Common situations

| What you see | What it means | What to do |
| --- | --- | --- |
| **"E-mail ou senha inválidos."** ("Invalid e-mail or password.") | The e-mail does not belong to an active account, the password is wrong, or the account has not been activated yet. The message is the same in all three cases, so nobody can find out which e-mails have an account | Check the e-mail, which stays filled in, and type the password again. If you never activated the account, open the activation e-mail in the demo inbox |
| **"Informe o e-mail"** ("Enter the e-mail") or **"E-mail inválido"** ("Invalid e-mail") below the field | The e-mail is empty or is not an address | Fix the e-mail. Nothing was sent |
| **"Informe a senha"** ("Enter the password") below the field | The password is empty | Type the password. Nothing was sent |
| **"Sem conexão com o servidor"** ("No connection to the server") | The system did not respond | Check that the containers are running and try again |
| The login screen appears again after some days | The 7-day session ended | Sign in again |

## Frequently asked questions

**I forgot my password. What now?**
Password recovery is not available in this version.

**My account is blocked for transactions. Can I still sign in?**
Yes. A blocked account can sign in and see its data; only moving money is refused.

**How do I sign out?**
Signing out arrives with FR-03. Until then, the session ends after 7 days, or when you clear the cookies of the site in the browser.

# Manual: create and activate your account

This manual shows how to create a PortWallet account and activate it through the link sent by e-mail. It describes the behavior in the [FR-01 spec](spec.md) from the point of view of someone using the system.

PortWallet opens in your browser's language when it is Portuguese, English, Spanish or French, and in Brazilian Portuguese otherwise. To change it, use the **Idioma** (Language) selector at the bottom of any page; the choice is remembered on that browser. The e-mails come in the language you registered in. Buttons, fields and messages are quoted here in Brazilian Portuguese, exactly as they appear on screen, followed by a translation.

> **Demo environment.** PortWallet moves no real money. Use fictitious data: any name, a made-up e-mail and a valid fictitious CPF (you can generate one with any CPF generator for testing).

## Before you start

With the project running through Docker (`docker compose up --build`), the system is at <http://localhost:3000>.

| What | Where |
| --- | --- |
| Home screen | <http://localhost:3000> |
| Registration | <http://localhost:3000/register> |
| Demo inbox (where e-mails arrive) | <http://localhost:3000/inbox> |

In this environment no e-mail is actually sent: every message stays in the **demo inbox**, which works as your e-mail client.

## Overview

1. You fill in the registration with name, e-mail and CPF. **There is no password at this step.**
2. The system sends an e-mail with an activation link.
3. You open the link, check your data and **create the password**.
4. The account is activated and your wallet is opened with a balance of R$ 0.00.

## Step 1: fill in the registration

1. On the home screen, click **Criar conta** ("Create account").
2. Fill in the fields:

   | Field | Rule |
   | --- | --- |
   | Nome completo (full name) | 2 to 100 characters |
   | E-mail | A valid address. Upper case and surrounding spaces are fixed automatically |
   | CPF | 11 digits with valid verification digits. The `000.000.000-00` mask is applied as you type |

3. Click **Criar conta**.

If a field is wrong, the message shows right below it and nothing is sent. Fix it and try again.

When the submission succeeds, the screen shows **"Verifique seu e-mail"** ("Check your e-mail") with the message:

> Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.
> ("If the data is correct, you will receive an e-mail to activate the account.")

This message is always the same, whether or not an account already exists for that e-mail or CPF. It is a protection: nobody can find out, through the registration screen, who already has a PortWallet account. What actually happened is only told by e-mail, to the owner of the data.

## Step 2: open the activation e-mail

1. Click the **caixa de entrada demo** ("demo inbox") link below the message (or go to <http://localhost:3000/inbox>).
2. Type the e-mail used in the registration and click **Ver e-mails** ("See e-mails").
3. Open the **"Ative sua conta PortWallet"** ("Activate your PortWallet account") message. It shows the registration data, for you to check:

   ```text
   Nome: Ana Souza Lima
   CPF: ***.***.***-25
   ```

4. If the data is yours, click **Abrir link** ("Open link").

The link is valid for **24 hours** and can be used **only once**.

## Step 3: check the data and create the password

The **Ativação da conta** ("Account activation") screen has two parts.

**Data check.** The "Estes são os seus dados?" ("Is this your data?") box shows the name and the CPF of the registration, partially hidden (for example, `Ana L****` and `***.***.***-25`).

- If the data is **not yours**, do not continue. Click **faça o cadastro de novo** ("register again") and register again with the same e-mail. A new link is sent and the current one stops working.

**Creating the password.**

1. In **Crie uma senha** ("Create a password"), type a password of 8 to 72 characters.
2. In **Repita a senha** ("Repeat the password"), type the same password.
3. Click **Ativar conta** ("Activate account").

When everything works, the screen shows **"Conta ativada. Você já pode entrar com o seu e-mail e a senha que acabou de criar."** ("Account activated. You can now sign in with your e-mail and the password you just created.") and the **Ir para o login** ("Go to login") button.

> Login arrives with FR-02. Until then, the **Ir para o login** button leads to a page that does not exist yet.

## Common situations

| What you see | What it means | What to do |
| --- | --- | --- |
| No e-mail in the demo inbox | The e-mail typed in the inbox is not the one used in the registration, or the e-mail or CPF already belong to an account | Check the address. If an account already uses this data, the warning goes to the account owner's e-mail |
| E-mail **"Tentativa de cadastro com os seus dados"** ("Registration attempt with your data") | Someone (maybe you) tried to register an e-mail or CPF that already has an account. No new account was created | If it was you, use the existing account. If it was not, nothing needs to be done |
| **"Link inválido ou expirado."** ("Invalid or expired link.") | The link was already used, is older than 24 hours or was replaced by a more recent registration | Register again with the same e-mail to receive a new link |
| Data that is not yours in the "Estes são os seus dados?" box | Someone registered your e-mail again with other data | Do not activate. Click **faça o cadastro de novo** and register your own data |
| **"As senhas não são iguais"** ("The passwords do not match") | The two password fields are different | Type the same password in both fields |
| **"Use pelo menos 8 caracteres"** ("Use at least 8 characters") or **"Senha longa demais"** ("Password too long") | The password is outside the allowed size | Use 8 to 72 characters. Accented letters count as two |
| **"Sem conexão com o servidor"** ("No connection to the server") with the **Tentar de novo** ("Try again") button | The system did not respond | Check that the containers are running and click **Tentar de novo** |

## Frequently asked questions

**I typed a wrong detail in the registration. What now?**
If the account is not activated yet, register again with the same e-mail and the correct data. Only the most recent link works.

**Why is the password not asked at registration?**
So that only whoever has access to the e-mail chooses the password. This way, someone who registers your e-mail before you cannot get into your account ([ADR 0005](../../adr/0005-password-set-on-activation.md)).

**Why does the link disappear from the address bar?**
For security: the link is removed from the address and from the browser history as soon as the page opens, so it cannot be reused.

**I opened the link again and the screen did not change.**
If you reopen, in the same tab, a link you just used, the screen keeps showing the activated account. In a new tab, the same link shows "Link inválido ou expirado.", because each link works only once.

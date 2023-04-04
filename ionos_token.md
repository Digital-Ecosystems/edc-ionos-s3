# IONOS's Token creation

This document explains how to create a token for authentication purposes.

## Requirements

You will need the following:
- IONOS account;
- script/curl to generate the authentication token;

Please, take a look at the [IONOS's API documentation](https://api.ionos.com/docs/authentication/v1/#tag/tokens).

## Token creation

We will see how to create the token using a curl command. The IONOS API will require your IONOS's account to do the authentication.

```console
curl -X GET -u <YOUR EMAIL>:<YOUR PASSWORD> "https://api.ionos.com/auth/v1/tokens/generate"

```

You will have an output like the following:

```
{"token":"WQiOiJmMTNhNWM1NC04MjI4LTRiN2UtOTQxNC02MTMxM2NlNzIzZDEiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpb25vc2Nsb3VkIiwiaWF0IjoxNjc5OTEwODk2LCJjbGllbnQiOiJVU0VSIiwiaWRlbnRpdHkiOnsidXVpZCI6ImI2NjA0NzU5LWVkYTgtNGNlYS05NGQwLTdkNDk1YjM5NzNkYSIsImNvbnRyYWN0TnVtYmVyIjozMTk1Mjc0NiwicmVzZWxsZXJJZCI6MSwicm9sZSI6Im93bmVyIiwicmVnRG9tYWluIjoiaW9ub3MuZGUiLCJwcml2aWxlZ2VzIjpbIkRBVEFfQ0VOVEVSX0NSRUFURSIsIlNOQVBTSE9UX0NSRUFURSIsIklQX0JMT0NLX1JFU0VSVkUiLCJNQU5BR0VfREFUQVBMQVRGT1JNIiwiQUNDRVNTX0FDVElWSVRZX0xPRyIsIlBDQ19DUkVBVEUiLCJBQ0NFU1NfUzNfT0JKRUNUX1NUT1JBR0UiLCJCQUNLVVBfVU5JVF9DUkVBVEUiLCJDUkVBVEVfSU5URVJORVRfQUNDRVNTIiwiSzhTX0NMVVNURVJfQ1JFQVRFIiwiRkxPV19MT0dfQ1JFQVRFIiwiQUNDRVNTX0FORF9NQU5BR0VfTU9OSVRPUklORyIsIkFDQ0VTU19BTkRfTUFOQUdFX0NFUlRJRklDQVRFUyIsIk1BTkFHRV9EQkFBUyIsIkFDQ0VTU19BTkRfTUFOQUdFX0ROUyIsIk1BTkFHRV9SRUdJU1RSWSJdLCJpc1BhcmVudCI6ZmFsc2V9LCJleHAiOjE3MTE0Njg0OTZ9.gmkZCVW0BCkmkiel8-E9iZW1IO9uVBfqVvqqr-qBG0L44QMsum65jDleMJhEm6EGocSDfPXHpV-9hSjcBII5oDNtWdytFRMAo_fWdC_8PsuCntvnqigkN8fAe-JcEuoT_kAArgS4Nc65iHXkMv2NGPA8Gber7gg40Nc7uKPXOPInK1U76Ks123uzZa0SuEzu7o8luD2D7QZ3i1PkVu75ZXuGLmt8EhKSFHRmS2eIw"}
```

You can now copy and use the token.
# Firebase/FCM setup for Sprint08

Project: the-discipline-476506-q7. Android applicationId: com.nihonix.mobile.
Firebase app:1:630174674961:android:b1c70cc5845a783ef841b0.
User supplied existing project/configuration and confirmed CLI sign-in.

Android config was retrieved through Firebase CLI apps:sdkconfig and placed in app/google-services.json after checking project/package. Google services plugin and Firebase Messaging dependency are configured. This client config contains public project/app identifiers; it is not a backend private key.

Backend local .env.develop references FIREBASE_PROJECT_ID and GOOGLE_APPLICATION_CREDENTIALS pointing to the user-supplied file outside either repository. The private key was not copied into source or printed. Ignore rules exclude firebase-adminsdk/service-account JSON files. No SMTP/AI credentials changed. A running backend must restart to reload environment values; setup does not restart the user's server automatically.

## app/google-services.json is git-ignored

The file is listed in .gitignore, so a fresh clone does not contain it. The Google services Gradle plugin fails the build when the file is missing. Before building, regenerate it with the Firebase CLI (`firebase apps:sdkconfig ANDROID <app id> --project <project id>`), save the output as app/google-services.json, and check that project_id and package_name match the values above. The file holds public project/app identifiers and an API key that should be restricted in Google Cloud; it is not a backend private key.

## Verification status

Google services processing and the debug build passed, and server credential readiness was checked with a validation-only request. No notification has been sent to a real user. In the Sprint09 emulator run the app obtained a real FCM token and registered it with a local backend; the provider on that backend was a fake. Foreground/background receipt on a physical device and real delivery remain open.

References:
- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/cloud-messaging/android/get-started
- https://firebase.google.com/docs/cloud-messaging/send/v1-api

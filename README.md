# PlaceWalQR

## Students:
- Cristian Andrei Mai Mihai, maimihai.1942925@studenti.uniroma1.it
- Michele Nicoletti, nicoletti.1886646@studenti.uniroma1.it
- Lorenzo Pecorari, pecorari.1885161@studenti.uniroma1.it

## Description of the app
The following application invites users to visit places around the Italy and many other nations.

Using the position provided by the GPS sensor inside the Android smartphone, that runs the app, it is possible to check the map provided by Google for exploring the next destination.

Once found a QR code in the place, the user can scan it through the dedicated fragment and, thanks to MLKit, the application can recognize the information behind the code captured by the camera.

Users can compete with others through the leaderboard: each place visited allows to cumulate a certain quantity of points.

All the places visited can be seen inside the dedicated fragment, showing collection and additional points when completed.

The login can be done according to the Google signin or with the customized registration provided as a service.

## Sensors
- GPS
- Camera
- "Haptic engine", vibration motor

## External API
- Google Maps
- Google Authentication

## Custom API
- Pythonanywhere with endpoints in Flask
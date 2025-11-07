# Room schema history

This directory stores the JSON schema snapshots that Room exports via the `room.schemaLocation`
annotation processor argument. The files are generated during compilation (for example when running
`./gradlew assembleDebug`). Commit them to version control so Room migrations can be validated in
tests.
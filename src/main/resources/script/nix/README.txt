This directory contains a script for installing @name@ as a service, starting
the service, and setting the service to start automatically on boot.

To install as the current user, run:

./install.sh

To install as a different user, run:

./install.sh someOtherUser

This will create the user if it does not already exist, and change ownership to that user

After adding the service, on linux it can be started via:

/etc/init.d/@name@ start

and stopped via:

/etc/init.d/@name@ stop

And on OS X:

Start:
launchctl load @name@

Stop:
launchctl unload @name@


REMOVAL

To remove the service:

./remove.sh

To remove the service and service user:

./remove.sh someOtherUser

Only the service will be removed, all application files will still be in place.
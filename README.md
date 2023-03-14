# qalipsis-plugin-mail

QALIPSIS supports email messaging, to notify users of the status of their campaigns after it runs. This is achieved by
configuring and enabling the QALIPSIS publisher for mail.

Below is a list of the configurations necessary to activate campaign report publishing by mail:

Configuration namespace: `report.export.mail`

##### Parameters:

- `enabled` (required): boolean flag that activates/deactivates campaign report publishing to mail; defaults to `false`; *must*
  be set to `true`.
- `status` (required): specifies which campaign report statuses should trigger a notification; allowable values is any
  set of `ReportExecutionStatus`values: ALL, FAILED, SUCCESSFUL, WARNING, ABORTED; defaults to `ALL`, which triggers
  notification for any of the campaign report statuses listed above; A combination of ABORTED and FAILED statuses, only
  triggers notification when the campaign report status is either ABORTED OR FAILED. Finally, selecting just one value
  e.g. SUCCESSFUL triggers notifications only when the campaign is SUCCESSFUL.
- `username`: username to be used in authenticating network connection requests when password authentication is
  required (when the AuthenticationMode is set to `USERNAME_PASSWORD`).
- `password`: password to be used in authenticating network connection requests when password authentication is
  required (when the AuthenticationMode is set to `USERNAME_PASSWORD`).
- `host`: the host name of the mail server to connect to; defaults to `localhost`.
- `port`: the port number of the mail server to connect to; defaults to `25`.
- `authenticationMode`: authentication options for SMTP server; allowable values is any set of `AuthenticationMode`
  values: USERNAME_PASSWORD, PLAIN; defaults to `PLAIN`.
- `from`: email to indicate sender address; defaults to `no-reply@@qalipsis.io`.
- `to` (required): list of valid email addresses that are recipients of the email.
- `cc`: list of valid email addresses to be copied in the email; defaults to an empty list.
- `junit`: boolean flag to activate/deactivate inclusion of Junit report as zipped attachments, if they exist; defaults
  to `false`.
- `ssl`: boolean flag to enable/disable use of SSL to connect; defaults to `false`.
- `starttls`: boolean flag to enable/disable use of the STARTTLS command (if supported by the server); defaults to `false`.

Below is a valid configuration to activate and enable campaign report publishing to slack:

```yaml
report:
  export:
    mail:
      enabled: true
      username: qalipsis-user
      password: passpass
      status: #Using a list of SUCCESS, FAILED and WARNING triggers notification only when 
        #the status of a campaign report is success, a warning or a failure. It ignores every other statuses
        - SUCCESS
        - FAILED
        - WARNING
      authenticationMode: USERNAME_PASSWORD #allows authentication requests for a network connection
      from: foo@bar.com
      to: 
        - fooadmin1@bar.com
        - fooadmin2@bar.com
      junit: false #Junit report attachments would not be included in the email.
      ssl: true
      starttls: true
      host: smtp.gmail.com
      port: 587
```

[This section of the QALIPSIS documentation](https://docs.qalipsis.io/#_external-property-sources ), gives insight on
how to go about adding new configuration properties that sets or override the default properties.

After these steps, you are all set to receive email message notifications in your configured inbox.
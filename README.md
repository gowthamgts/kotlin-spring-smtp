### Kotlin James SMTP Spring boot starter

This repo contains a starter pack for James SMTP server. SMTP server will start alongside the embedded tomcat via spring boot configuration lifecycle.

This contains the following packages:
- `hooks` - this is where your SMTP hooks will live. For more information refer - https://james.apache.org/server/feature-smtp-hooks.html
- `configs` - package where springboot performs configuration property scans. This package contains the SMTP server init code.

#### SMTP Server configurations

You can configure the following two properties file:
- `resources/smtp/smtpserver.xml` - refer https://james.apache.org/server/config.html
- `resources/smtp/dnsserver.xml` - refer https://james.apache.org/server/config.html

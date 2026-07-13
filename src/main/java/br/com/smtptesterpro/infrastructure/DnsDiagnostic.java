package br.com.smtptesterpro.infrastructure;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public final class DnsDiagnostic {
    public List<String> lookupMx(String domain) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            var context = new InitialDirContext(environment);
            var attributes = context.getAttributes(domain, new String[]{"MX"});
            var mx = attributes.get("MX");
            if (mx == null) {
                return List.of();
            }

            List<String> records = new ArrayList<>();
            NamingEnumeration<?> values = mx.getAll();
            while (values.hasMore()) {
                records.add(values.next().toString());
            }
            records.sort(String::compareToIgnoreCase);
            return records;
        } catch (NamingException exception) {
            throw new IllegalStateException("Falha ao consultar DNS MX: " + exception.getMessage(), exception);
        }
    }
}

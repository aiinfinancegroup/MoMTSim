package org.momtsim.identity;

import com.devskiller.jfairy.Bootstrap;
import com.devskiller.jfairy.Fairy;
import com.devskiller.jfairy.producer.company.Company;
import com.devskiller.jfairy.producer.person.Person;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Wraps the jFairy library and provides an identity generation function.
 * <p>
 * This keeps some of the jFairy confusion to a minimum.
 */
public class IdentityFactory {
    private final Set<String> ccnSet = new HashSet<>();
    private final Set<String> merchantIdSet = new HashSet<>();

    final private Fairy fairy;

    public IdentityFactory(int randomSeed) {
        fairy = Bootstrap.builder()
                .withRandomSeed(randomSeed)
                .withLocale(Locale.US)
                .withLocale(Locale.CANADA).build();
    }

    protected String addSuffixToEmail(String email, String suffix) {
        String[] parts = email.split("@");
        assert parts.length == 2;
        return parts[0] + suffix + "@" + parts[1];
    }

    public ClientIdentity nextPerson() {
        Person p = fairy.person();

        return new ClientIdentity(
                getNextCreditCard(),
                p.getFullName(),
                addSuffixToEmail(p.getEmail(), p.getNationalIdentityCardNumber().substring(0, 3)),
                p.getNationalIdentityCardNumber(),
                p.getTelephoneNumber());
    }

    public BankIdentity nextBank() {
        Person p = fairy.person();
        return new BankIdentity(getNextVAT(), String.format("Bank of %s", p.getLastName()));
    }

    public MerchantIdentity nextMerchant() {
        int i = 0;
        Company c = fairy.company();
        while (!merchantIdSet.add(c.getVatIdentificationNumber())) {
            // If we can't find a unique id after a handful of attempts, just use a number
            // suffix to hopefully get something unique. Yes, this is hacky.
            c = new Company(c.getName(), c.getDomain(), c.getEmail(),
                    String.format("%s-%d", c.getVatIdentificationNumber(), merchantIdSet.size()));
        }
        return new MerchantIdentity(c.getVatIdentificationNumber(), c.getName());
    }

    public String getNextVAT() {
        Company c = fairy.company();
        return c.getVatIdentificationNumber();
    }

    public String getNextCreditCard() {
        String ccn = fairy.creditCard().getCardNumber();
        while (!ccnSet.add(ccn)) {
            ccn = fairy.creditCard().getCardNumber();
        }
        return ccn;
    }

    public String nextMerchantName() {
        Company c = fairy.company();
        return c.getName();
    }
}

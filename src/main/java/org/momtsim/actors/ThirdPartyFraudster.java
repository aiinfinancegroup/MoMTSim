package org.momtsim.actors;

import org.momtsim.MoMTSimState;
import org.momtsim.base.Transaction;
import org.momtsim.identity.ClientIdentity;
import org.momtsim.identity.HasClientIdentity;
import org.momtsim.identity.Identifiable;
import org.momtsim.identity.Identity;
import org.momtsim.output.Output;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.*;


/**
 * Hi all, I'm Theo...the 3rd Party Fraudster. This code implements the direct deposit fraudulent scheme in MMTs
 */
public class ThirdPartyFraudster extends SuperActor implements HasClientIdentity, Identifiable, Steppable {
    private double profit = 0;
    private final ClientIdentity identity;
    private final Mule mule;
    private final Set<Client> victims;
    private final Set<Merchant> favoredMerchants;

    public ThirdPartyFraudster(MoMTSimState state, ClientIdentity identity) {
        super(state);
        this.identity = identity;
        victims = new HashSet<>();
        favoredMerchants = new HashSet<>();

        mule = new Mule(state, identity);
        state.addClient(mule);
    }

    /**
     * These are the merchants I've found a way to breach...or maybe in cahoots with.
     *
     * @param m a Merchant in the simulation
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean addFavoredMerchant(Merchant m) {
        return favoredMerchants.add(m);
    }

    protected double pickTestChargeAmount(MoMTSimState state, Client victim, String actionType) {
        final double wobble = 1 + (1f / (state.getRNG().nextInt(50) + 1));
        final double avgAmountForAction = victim.getClientProfile().getProfilePerAction(actionType).getAvgAmount();
        return avgAmountForAction * 0.25 * wobble;
    }

    protected Merchant pickTestMerchant(MoMTSimState state) {
        final int merchantPopulation = state.getMerchants().size();
        Merchant m = state.getMerchants().get(state.getRNG().nextInt(merchantPopulation));
        while (favoredMerchants.contains(m)) {
            m = state.getMerchants().get(state.getRNG().nextInt(merchantPopulation));
        }
        return m;
    }

    protected Optional<Merchant> pickFavoredMerchant(MoMTSimState state) {
        final int numMerchants = favoredMerchants.size();
        if (numMerchants > 0) {
            final int choice = state.getRNG().nextInt(numMerchants);
            return Optional.of(favoredMerchants.toArray(new Merchant[numMerchants])[choice]);
        }
        return Optional.empty();
    }

    protected Client pickTargetClient(MoMTSimState state) {
        Optional<Merchant> maybeMerchant = pickFavoredMerchant(state);
        Merchant m = maybeMerchant.orElse(state.pickRandomMerchant());

        if (m.getRecentClients().size() > 1) {
            Client c = m.getRecentClients().get(state.getRNG().nextInt(m.getRecentClients().size()));
            if (c.getId() != this.getId()) {
                // XXX: In practice this may not happen since we currently don't let Fraudsters perform
                // transactions with anyone directly, but just to be safe.
                return c;
            }
        }
        return state.pickRandomClient(getId());
    }

    protected Optional<Client> pickRepeatVictim(MoMTSimState state) {
        final int numVictims = victims.size();
        if (numVictims > 0) {
            final int choice = state.getRNG().nextInt(numVictims);
            return Optional.of(victims.toArray(new Client[numVictims])[choice]);
        }
        return Optional.empty();
    }

    @Override
    public Type getType() {
        return Type.THIRD_PARTY_FRAUDSTER;
    }

    //Core logic for Direct deposit fraudelent activity in MMTs

    @Override
    public void step(SimState state) {
        MoMTSimState paysim = (MoMTSimState) state;
        ArrayList<Transaction> transactions = new ArrayList<>();
        int step = (int) state.schedule.getSteps();

        // Implement new fraudulent behavior
        if (paysim.getRNG().nextDouble() < parameters.thirdPartyFraudProbability) {
            // Pick a target client and a merchant
            Client targetClient = pickTargetClient(paysim);
            Merchant selectedMerchant = pickTestMerchant(paysim);

            // Calculate the deposit amount using CASH-IN transaction type
            double depositAmount = pickTestChargeAmount(paysim, targetClient, Client.CASH_IN);

            // Make a deposit into the target client's account via the selected merchant
            Transaction deposit = targetClient.handleCashIn(selectedMerchant, step, depositAmount);

            // If the random number generated is less than 0.3, label the transaction as fraudulent
            if (paysim.getRNG().nextDouble() < 0.3) {
                deposit.setFraud(true);
            }
            // Add the transaction to the list of transactions
            transactions.add(deposit);
        }
        // Check mule account for possible cash-outs
        if (paysim.getRNG().nextBoolean(0.3)) {
            mule.fraudulentCashOut(paysim, step);
        }

        // Inform the simulation about the transactions
        paysim.onTransactions(transactions);
    }


    @Override
    public String toString() {
        ArrayList<String> properties = new ArrayList<>();
        final Set<String> uniqueVictims = new HashSet<>();
        victims.forEach(v -> uniqueVictims.add(v.getId()));

        properties.add(getId());
        properties.add(getType().toString());
        properties.add(Integer.toString(uniqueVictims.size()));
        properties.add(String.format("[%s]", String.join(",", uniqueVictims)));
        properties.add(Output.fastFormatDouble(Output.PRECISION_OUTPUT, profit));

        return String.join(Output.OUTPUT_SEPARATOR, properties);
    }

    @Override
    public ClientIdentity getClientIdentity() {
        return identity;
    }

    @Override
    public String getId() {
        return identity.id;
    }

    @Override
    public String getName() {
        return identity.name;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public Map<String, Object> getIdentityAsMap() {
        return identity.asMap();
    }
}

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
 * Hi all, ...this is Fraudster that performs split deposit fraud in MMTs from SSA.
 * This implements the Split Agent Deposit fraud scenario for MMTs in SSA
 * Core logic or fraudulent behaviour in the split agent deposit is found in the step method
 */
public class SplitDepositFraudster extends SuperActor implements HasClientIdentity, Identifiable, Steppable {
    private double profit = 0;
    private final ClientIdentity identity;
    private final Mule mule;
    private final Set<Client> victims;
    private final Set<Merchant> favoredMerchants;

    public SplitDepositFraudster(MoMTSimState state, ClientIdentity identity) {
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


    //Core Logic for Split Agent Deposit Fraud
    @Override
    public void step(SimState state) {
        MoMTSimState momtsim = (MoMTSimState) state;
        ArrayList<Transaction> transactions = new ArrayList<>();
        int step = (int) state.schedule.getSteps();

        // Cash-In Fraud Logic
        if (momtsim.getRNG().nextDouble() < parameters.thirdPartyFraudProbability) {
            // Choose a random favored merchant or a random merchant from the simulation
            Merchant selectedMerchant = pickFavoredMerchant(momtsim).orElse(momtsim.pickRandomMerchant());

            // Determine the number of Cash-In transactions to perform
            int numberOfCashIns = momtsim.getRNG().nextInt(10) + 1;

            for (int i = 0; i < numberOfCashIns; i++) {
                // Select a random client
                Client c = pickTargetClient(momtsim);

                // Calculate a Cash-In amount based on the client's profile and action type
                final double cashInAmount = pickTestChargeAmount(momtsim, c, Client.CASH_IN);

                // Create a Cash-In transaction using the client, selected merchant, and amount
                Transaction cashInTransaction = c.handleCashIn(selectedMerchant, step, cashInAmount);
                cashInTransaction.setFraud(true);

                // Add the Cash-In transaction to the transactions list
                transactions.add(cashInTransaction);
            }
        }

        // Mule account cash-out check remains the same as before
        if (momtsim.getRNG().nextBoolean(0.3)) {
            mule.fraudulentCashOut(momtsim, step);
        }
        momtsim.onTransactions(transactions);
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

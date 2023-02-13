package org.momtsim.actors.networkdrugs;

import ec.util.MersenneTwisterFast;
import org.momtsim.MoMTSimState;
import org.momtsim.actors.Client;
import org.momtsim.base.Transaction;
import org.momtsim.utils.RandomCollection;
import sim.engine.SimState;

public class DrugConsumer extends Client {
    private DrugDealer dealer;
    private RandomCollection<Double> probAmountProfile;
    private double probabilityBuy;

    public DrugConsumer(MoMTSimState paySim, DrugDealer dealer, double monthlySpending, RandomCollection<Double> probAmountProfile, double meanTr) {
        super(paySim);
        this.dealer = dealer;
        this.probAmountProfile = probAmountProfile;
        this.probabilityBuy = monthlySpending / meanTr / paySim.getParameters().nbSteps;
    }

    @Override
    public void step(SimState state) {
        MoMTSimState paySim = (MoMTSimState) state;
        int step = (int) paySim.schedule.getSteps();

        super.step(state);

        if (wantsToBuyDrugs(paySim.random)) {
            double amount = pickAmount();

            handleTransferDealer(step, amount);
        }
    }

    private Transaction handleTransferDealer(int step, double amount) {
        Transaction t = handleTransfer(dealer, step, amount);

        if (t.isSuccessful()) {
            dealer.addMoneyFromDrug(amount);
        }

        return t;
    }

    private boolean wantsToBuyDrugs(MersenneTwisterFast random) {
        return random.nextBoolean(probabilityBuy);
    }

    private double pickAmount() {
        return probAmountProfile.next();
    }
}

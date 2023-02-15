package org.momtsim;

import org.momtsim.base.Transaction;
import org.momtsim.output.Output;
import org.momtsim.parameters.Parameters;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MoMTSimApp extends MoMTSimState {

    private static final String[] DEFAULT_ARGS = new String[]{"", "-file", "MoMTSim.properties", "1"};

    public final String simulationName;
    private int totalTransactionsMade = 0;
    private int stepParticipated = 0;

    private List<Transaction> transactions = new ArrayList<>();
    private int currentStep;

    public static void main(String[] args) {
        System.out.println("MoMTSim: Financial Simulator v" + MOMTSIM_VERSION);
        if (args.length < 4) {
            args = DEFAULT_ARGS;
        }
        int nbTimesRepeat = Integer.parseInt(args[3]);
        String propertiesFile = "";
        for (int x = 0; x < args.length - 1; x++) {
            if (args[x].equals("-file")) {
                propertiesFile = args[x + 1];
            }
        }
        Parameters parameters = new Parameters(propertiesFile);
        for (int i = 0; i < nbTimesRepeat; i++) {
            MoMTSimApp p = new MoMTSimApp(parameters);
            p.run();
        }
    }

    public MoMTSimApp(Parameters parameters) {
        super(parameters);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date currentTime = new Date();
        simulationName = "SD_" + dateFormat.format(currentTime) + "_" + seed();

        File simulationFolder = new File(parameters.outputPath + simulationName);
        simulationFolder.mkdirs();

        Output.initOutputFilenames(simulationName, parameters.outputPath);
        Output.writeParameters(parameters);
    }

    @Override
    public boolean onTransactions(List<Transaction> transactions) {
        return this.transactions.addAll(transactions);
    }

    @Override
    protected boolean onStep(long stepNum) {
        if (stepNum > Integer.MAX_VALUE) {
            System.err.println(String.format("Reached MAX_INT number of steps (%d). Aborting.", Integer.MAX_VALUE));
            return false;
        }
        currentStep = (int) stepNum + 1;
        writeOutputStep();

        if (stepNum % 100 == 100 - 1) {
            System.out.println(" Step " + currentStep);
        } else {
            System.out.print("*");
        }
        return true;
    }

    @Override
    public void run() {
        System.out.println("\nStarting MoMTSim Running for " + parameters.nbSteps + " steps.");
        long startTime = System.currentTimeMillis();

        runSimulation();

        System.out.println("\nFinished running " + currentStep + " steps ");
        finish();

        double total = System.currentTimeMillis() - startTime;
        total = total / 1000 / 60;
        System.out.println("It took: " + total + " minutes to execute the simulation");
        System.out.println("Simulation name: " + simulationName);
        System.out.println();
    }

    public void finish() {
        Output.writeFraudsters(fraudsters);
        Output.writeClientsProfiles(countProfileAssignment, (int) (parameters.nbClients * parameters.multiplier));
        Output.writeSummarySimulation(this);
    }

    private void resetVariables() {
        if (transactions.size() > 0) {
            stepParticipated++;
        }
        transactions = new ArrayList<>();
    }

    private void writeOutputStep() {
        List<Transaction> transactions = getTransactions();

        totalTransactionsMade += transactions.size();

        Output.incrementalWriteRawLog(currentStep, transactions);
        if (parameters.saveToDB) {
            Output.writeDatabaseLog(parameters.dbUrl, parameters.dbUser, parameters.dbPassword, transactions, simulationName);
        }

        Output.incrementalWriteStepAggregate(currentStep, transactions);
        resetVariables();
    }

    public int getTotalTransactions() {
        return totalTransactionsMade;
    }

    public int getStepParticipated() {
        return stepParticipated;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
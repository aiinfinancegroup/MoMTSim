package org.momtsim.output;

import org.momtsim.MoMTSimApp;
import org.momtsim.actors.SuperActor;
import org.momtsim.base.ClientActionProfile;
import org.momtsim.base.StepActionProfile;
import org.momtsim.base.Transaction;
import org.momtsim.parameters.Parameters;
import org.momtsim.parameters.StepsProfiles;
import org.momtsim.utils.DatabaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Output {
    public static final int PRECISION_OUTPUT = 2;
    public static final String OUTPUT_SEPARATOR = ",", EOL_CHAR = System.lineSeparator();
    private static String filenameGlobalSummary, filenameParameters, filenameSummary, filenameRawLog,
            filenameStepAggregate, filenameClientProfiles, filenameFraudsters;

    private static final Logger logger = LoggerFactory.getLogger(Output.class);

    public static void incrementalWriteRawLog(long step, List<Transaction> transactions) {
        String rawLogHeader = "step,action,amount,nameOrig,oldBalanceOrig,newBalanceOrig,nameDest,oldBalanceDest,newBalanceDest,isFraud,isFlaggedFraud,isUnauthorizedOverdraft";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameRawLog, true));
            if (step == 0) {
                writer.write(rawLogHeader);
                writer.newLine();
            }
            for (Transaction t : transactions) {
                writer.write(t.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void incrementalWriteStepAggregate(long step, List<Transaction> transactions) {
        String stepAggregateHeader = "action,month,day,hour,count,sum,avg,std,step";
        Map<String, StepActionProfile> stepRecord = Aggregator.generateStepAggregate(step, transactions);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameStepAggregate, true));
            if (step == 0) {
                writer.write(stepAggregateHeader);
                writer.newLine();
            }
            for (StepActionProfile actionRecord : stepRecord.values()) {
                writer.write(actionRecord.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void writeFraudsters(List<SuperActor> fraudsters) {
        String fraudsterHeader = "name,type,nbVictims,victims,profit";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameFraudsters));
            writer.write(fraudsterHeader);
            writer.newLine();
            for (SuperActor f : fraudsters) {
                writer.write(f.toString());
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeParameters(Parameters parameters) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameParameters));
            writer.write(parameters.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeClientsProfiles(Map<ClientActionProfile, Integer> countPerClientActionProfile, int numberClients) {
        String clientsProfilesHeader = "action,high,low,total,freq";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameClientProfiles));
            writer.write(clientsProfilesHeader);
            writer.newLine();

            for (Map.Entry<ClientActionProfile, Integer> counterActionProfile : countPerClientActionProfile.entrySet()) {
                ClientActionProfile clientActionProfile = counterActionProfile.getKey();
                String action = clientActionProfile.getAction();
                int count = counterActionProfile.getValue();

                double probability = ((double) count) / numberClients;

                writer.write(action + "," + clientActionProfile.getMinCount() + "," + clientActionProfile.getMaxCount() + ","
                        + count + "," + fastFormatDouble(5, probability));
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeSummarySimulation(MoMTSimApp momtSim) {

        StringBuilder errorSummary = new StringBuilder();
        Parameters parameters = momtSim.getParameters();
        StepsProfiles simulationStepsProfiles = new StepsProfiles(Output.filenameStepAggregate, 1 / parameters.multiplier, parameters.nbSteps);
        double totalErrorRate = SummaryBuilder.buildSummary(parameters.stepsProfiles, simulationStepsProfiles, errorSummary);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Output.filenameSummary));
            writer.write(errorSummary.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String summary = momtSim.simulationName + "," + parameters.nbSteps + "," + momtSim.getTotalTransactions() + "," +
                momtSim.getClients().size() + "," + totalErrorRate;
        writeGlobalSummary(summary);

        logger.info("Nb of clients: " + momtSim.getClients().size() + " - Nb of steps with transactions: " + momtSim.getStepParticipated());
    }

    private static void writeGlobalSummary(String summary) {
        String header = "name,steps,nbTransactions,nbClients,totalError";
        File f = new File(filenameGlobalSummary);
        boolean fileExists = f.exists();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f, true));
            if (!fileExists) {
                writer.write(header);
                writer.newLine();
            }
            writer.write(summary);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeDatabaseLog(String dbUrl, String dbUser, String dbPassword,
                                        List<Transaction> transactions, String simulatorName) {
        DatabaseHandler handler = new DatabaseHandler(dbUrl, dbUser, dbPassword);
        for (Transaction t : transactions) {
            handler.insert(simulatorName, t);
        }
    }

    //See https://stackoverflow.com/a/10554128
    private static final int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000};

    public static String fastFormatDouble(int precision, double val) {
        StringBuilder sb = new StringBuilder();
        if (val < 0) {
            sb.append('-');
            val = -val;
        }
        int exp = POW10[precision];
        long lval = (long) (val * exp + 0.5);
        sb.append(lval / exp).append('.');
        long fval = lval % exp;
        for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
            sb.append('0');
        }
        sb.append(fval);
        return sb.toString();
    }

    public static String formatBoolean(boolean bool) {
        return bool ? "1" : "0";
    }

    public static void initOutputFilenames(String simulatorName, String outputPath) {
        String outputBaseString = outputPath + simulatorName + "//" + simulatorName;
        filenameGlobalSummary = outputPath + "summary.csv";

        filenameParameters = outputBaseString + "_MoMTSim.properties";
        filenameSummary = outputBaseString + "_Summary.txt";

        filenameRawLog = outputBaseString + "_rawLog.csv";
        filenameStepAggregate = outputBaseString + "_aggregatedTransactions.csv";
        filenameClientProfiles = outputBaseString + "_clientsProfiles.csv";
        filenameFraudsters = outputBaseString + "_fraudsters.csv";
    }
}

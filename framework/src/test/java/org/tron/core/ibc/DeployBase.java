package org.tron.core.ibc;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.testng.util.Strings;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.StringUtil;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.io.*;
import java.util.Map;

public class DeployBase {


    private static String abiPath = "/Users/quan/tron/java-tron/framework/src/test/java/org/tron/core/ibc/abi/";

    public static String deploy(AutoDeploy.Contract contract,
                                WalletGrpc.WalletBlockingStub blockingStubFull,
                                String key) throws IOException {
        long maxFeeLimit = 5000000000l;
        long originEnergyLimit = 1000000000;

        String contractAddress;
        if (contract.getMethod().isEmpty()) {
            byte[] bytes = PublicMethed.deployContract(contract.getName(), contract.getAbi(), contract.getCode(), "", maxFeeLimit,
                    0L, 0, originEnergyLimit, "0",
                    0, null, key, getAddress(key),
                    blockingStubFull);
            contractAddress = StringUtil.encode58Check(bytes);
        } else {
            System.out.println("deploy with param");
            byte[] bytes = PublicMethed.deployContractWithConstantParam(contract.getName(), contract.getAbi(), contract.getCode(), contract.getMethod(), contract.getParam(), "", maxFeeLimit,
                    0L, 0, originEnergyLimit, "0",
                    0, null, key, getAddress(key),
                    blockingStubFull);
            contractAddress = StringUtil.encode58Check(bytes);
        }

        PublicMethed.waitProduceNextBlock(blockingStubFull);
        return contractAddress;
    }

    public static String getCode(AutoDeploy.Contract contract) throws IOException {
        File file = new File(contract.getPath());
        String content = FileUtils.readFileToString(file);
        Map<String, Object> jsonObject = JsonUtil.json2Obj(content, Map.class);
        String code = jsonObject.get("bytecode").toString();
        if (Strings.isNullOrEmpty(code)) {
            throw new IOException(contract.getName() + ": code is null");
        }
        code = code.substring(2);
        return code;
    }

    public static String getAbi(AutoDeploy.Contract contract) throws IOException {
        File file = new File(abiPath + contract.getName());
        BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        while ((str = in.readLine()) != null) {
            return str;
        }
        return "";
    }

//    public static String getAbi(Contract contract) throws IOException {
//        if (!contract.name.contains("Proxy")) {
//            File file = new File(contract.path);
//            String content = FileUtils.readFileToString(file);
//            Map<String, Object> jsonObject = JsonUtil.json2Obj(content, Map.class);
//            String abi = jsonObject.get("abi").toString();
//            if (Strings.isNullOrEmpty(abi)) {
//                throw new IOException(contract.name + ": abi is null");
//            }
//            return abi;
//        } else {
//            return getAbiFromProxy(contract);
//        }
//    }

    public static String getAbiFromProxy(AutoDeploy.Contract contract) throws IOException {
        File file = new File(contract.getPath());
        String content = FileUtils.readFileToString(file);
        Map<String, Object> jsonObject = JsonUtil.json2Obj(content, Map.class);
        String abi = jsonObject.get("abi").toString();
        if (Strings.isNullOrEmpty(abi)) {
            throw new IOException(contract.getName() + ": abi is null");
        }

        // get implemetation abi
        String implPath = "";
        if (contract.getName().contains("Proxy")) {
            implPath = contract.getPath().replaceFirst("Proxy", "");
        }
        File fileImpl = new File(implPath);
        String contentImpl = FileUtils.readFileToString(fileImpl);
        Map<String, Object> jsonObjectImpl = JsonUtil.json2Obj(contentImpl, Map.class);
        String abiImpl = jsonObjectImpl.get("abi").toString();
        if (Strings.isNullOrEmpty(abi)) {
            throw new IOException(contract.getName() + ": abi is null");
        }

//        JSONArray jsonArray1 = JSONArray.parseArray(abi);
//        JSONArray jsonArray2 = JSONArray.parseArray(abiImpl);
//        JSONArray jsonArray = new JSONArray();
//        jsonArray.addAll(jsonArray1);
//        jsonArray.addAll(jsonArray2);
        System.out.println(abi);
        System.out.println(abiImpl);
        abi = abi.substring(1, abi.length()-1);
        abiImpl = abiImpl.substring(0, abiImpl.length()-1);
        abiImpl += ",";
        abiImpl += abi;
        abiImpl += "]";
        System.out.println(abiImpl);
        return abiImpl;
    }

    private static byte[] getAddress(String priKey) {
        return ECKey.fromPrivate(ByteArray.fromHexString(priKey)).getAddress();
    }

    public static void main(String[] args) throws IOException {
//        getCode(Contract.Merkle);
        getAbi(AutoDeploy.Contract.GovernanceProxy);
    }



}

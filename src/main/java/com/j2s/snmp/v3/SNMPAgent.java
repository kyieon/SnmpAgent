//package com.j2s.snmp.v3;
//
//import lombok.extern.slf4j.Slf4j;
//import org.snmp4j.MessageDispatcher;
//import org.snmp4j.MessageDispatcherImpl;
//import org.snmp4j.Session;
//import org.snmp4j.TransportMapping;
//import org.snmp4j.agent.*;
//import org.snmp4j.agent.io.MOInputFactory;
//import org.snmp4j.agent.mo.DefaultMOFactory;
//import org.snmp4j.agent.mo.snmp.*;
//import org.snmp4j.agent.mo.snmp.dh.DHKickstartParameters;
//import org.snmp4j.agent.security.MutableVACM;
//import org.snmp4j.cfg.EngineBootsCounterFile;
//import org.snmp4j.cfg.EngineBootsProvider;
//import org.snmp4j.mp.CounterSupport;
//import org.snmp4j.mp.MPv3;
//import org.snmp4j.security.*;
//import org.snmp4j.smi.*;
//import org.snmp4j.transport.TransportMappings;
//import org.snmp4j.util.SnmpConfigurator;
//import org.snmp4j.util.ThreadPool;
//
//import java.io.File;
//import java.util.*;
//
//@Slf4j
//public class SNMPAgent {
//
//    protected AgentConfigManager agent;
//    protected DefaultMOServer moServer;
//    protected Modules modules;
//    protected OctetString context = new OctetString("context3");
//
//
//    public SNMPAgent() {
//        this("127.0.0.1");
//    }
//
//
//    public SNMPAgent(String address) {
//
//        List<String> listenAddress = Collections.singletonList(address);
//
//        // Initialize the server
//        this.moServer = new DefaultMOServer();
//        MOServer[] moServers = new MOServer[]{
//                moServer
//        };
//
//        File configFile = new File("default.config");
//        File bootCounterFile = new File("bootCounter.txt");
//
//
//        EngineBootsCounterFile engineBootsCounterFile = new EngineBootsCounterFile(bootCounterFile);
//        OctetString ownEngineId = engineBootsCounterFile.getEngineId(new OctetString(MPv3.createLocalEngineID()));
//
//        setupAgent(moServers, engineBootsCounterFile, ownEngineId, listenAddress);
//    }
//
//
//    private void setupAgent(MOServer[] moServers, EngineBootsProvider engineBootsProvider, OctetString engineID, List<String> listenAddress) {
//        try {
//
//            MessageDispatcher messageDispatcher = new MessageDispatcherImpl();
//            addListenAddresses(messageDispatcher, listenAddress);
//
//            // Uncomment to create and configure persistent provider
//            // DefaultMOPersistenceProvider persistenceProvider = new DefaultMOPersistenceProvider(new MOServer[]{server}, configFile.getAbsolutePath());
//
//            Collection<DHKickstartParameters> dhKickstartParameters = Collections.emptyList();
////            if (dhKickstartInfoPath != null) {
////                File dhKickstartInfoFile = new File(dhKickstartInfoPath);
////                if (dhKickstartInfoFile.canRead()) {
////                    try {
////                        Properties kickstartProperties = new Properties();
////                        FileInputStream fileInputStream = new FileInputStream(dhKickstartInfoFile);
////                        kickstartProperties.load(fileInputStream);
////                        fileInputStream.close();
////                        dhKickstartParameters =
////                                DHKickstartParametersImpl.readFromProperties("org.snmp4j.", kickstartProperties);
////                    } catch (IOException iox) {
////                        log.error("Failed to load Diffie Hellman kickstart parameters from '" +
////                                dhKickstartInfoPath + "': " + iox.getMessage(), iox);
////                    }
////                } else {
////                    log.warn("Diffie Hellman kickstart parameters file cannot be read: " + dhKickstartInfoFile);
////                }
////            }
//
//            SnmpConfigurator snmpConfigurator = new SnmpConfigurator(true);
//
//            MOInputFactory configurationFactory = null;
//            VacmMIB customViews = getCustomViews(moServers);
//
//
//            Map<String, List<Object>> securitySettings = new HashMap<>();
//            securitySettings.put("oSecurityName", Collections.singletonList("user"));
//            securitySettings.put("oAuthPassphrase", Collections.singletonList("userAuthPassword"));
//            securitySettings.put("oPrivPassphrase", Collections.singletonList("userPrivPassword"));
//            securitySettings.put("oAuthProtocol", Collections.singletonList("MD5"));
//            securitySettings.put("oPrivProtocol", Collections.singletonList("DES"));
//
//            AgentConfigManager agent = new AgentConfigManager(engineID, messageDispatcher, customViews, moServers, ThreadPool.create("SampleAgent", 4), configurationFactory, null, engineBootsProvider, null, dhKickstartParameters) {
//
//                @Override
//                protected Session createSnmpSession(MessageDispatcher dispatcher) {
//                    Session session = super.createSnmpSession(dispatcher);
//                    snmpConfigurator.configure(session, getUsm(), messageDispatcher, securitySettings);
//                    return session;
//                }
//            };
//            System.out.println("AgentConfigManager initialized successfully.");
//            agent.setContext(new SecurityModels(),
//                    new SecurityProtocols(SecurityProtocols.SecurityProtocolSet.maxCompatibility), new CounterSupport());
//
//        } catch (Exception e) {
//            log.error("Error setting up the agent: ", e);
//        }
//    }
//
//
//    public VacmMIB getCustomViews(MOServer[] moServers) {
//        VacmMIB vacm = new VacmMIB(moServers);
//        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString("context3"), new OctetString("v1v2group"),
//                StorageType.nonVolatile);
//        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("user"), new OctetString("v3group"),
//                StorageType.nonVolatile);
//        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("user1"), new OctetString("v3group"),
//                StorageType.nonVolatile);
//        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("user2"), new OctetString("v3group"),
//                StorageType.nonVolatile);
//        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("user3"), new OctetString("v3group"),
//                StorageType.nonVolatile);
//        vacm.addGroup(SecurityModel.SECURITY_MODEL_TSM, new OctetString(""), new OctetString("v3group"),
//                StorageType.nonVolatile);
//        vacm.addAccess(new OctetString("v1v2group"), new OctetString("context3"), SecurityModel.SECURITY_MODEL_ANY,
//                SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
//                new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
//
//        vacm.addAccess(new OctetString("v3group"), context, SecurityModel.SECURITY_MODEL_USM,
//                SecurityLevel.AUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
//                new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
//
//        vacm.addAccess(new OctetString("v3group"), context, SecurityModel.SECURITY_MODEL_TSM,
//                SecurityLevel.AUTH_PRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
//                new OctetString("fullWriteView"), null, StorageType.nonVolatile);
//
//        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"), new OctetString(),
//                VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
//        vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"), new OctetString(),
//                VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
//        vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"), new OctetString(),
//                VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
//        return vacm;
//    }
//
//    protected void addListenAddresses(MessageDispatcher md, List<String> addresses) {
//        for (String addressStr : addresses) {
//            System.out.println("Address being processed: " + addressStr);
//            Address address = GenericAddress.parse(addressStr);
//            System.out.println("Parsed Address : " + address);
//            System.out.println("Parsed address type: " + address.getClass().getName());
//
//            if (address == null) {
//                log.error("Could not parse address string '" + addressStr + "'");
//                return;
//            }
//            TransportMapping<? extends Address> tm =
//                    TransportMappings.getInstance().createTransportMapping(address);
//            System.out.println("Transpot mapping : " + tm);
//            if (tm != null) {
//                md.addTransportMapping(tm);
//                System.out.println("Transpot mapping successfull!" + address);
//            } else {
//                log.warn("No transport mapping available for address '" +
//                        address + "'.");
//            }
//        }
//    }
//
//    protected void registerMIBs() {
//        if (modules == null) {
//            modules = new Modules(DefaultMOFactory.getInstance());
//        }
//        try {
//            modules.registerMOs(moServer, null);
//        } catch (DuplicateRegistrationException drex) {
//            log.error("Duplicate registration: " + drex.getMessage() + "." +
//                    " MIB object registration may be incomplete!", drex);
//        }
//    }
//
//    public void start() {
//        moServer.addContext(context);
//        agent.initialize();
////        addUsmUser(agent.getUsm());
//        agent.setupProxyForwarder();
//        registerMIBs();
//        agent.registerShutdownHook();
//        agent.run();
//    }
//}
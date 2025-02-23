/*_############################################################################
  _## 
  _##  SNMP4J-Agent 3 - SampleAgent.java  
  _## 
  _##  Copyright (C) 2005-2018  Frank Fock (SNMP4J.org)
  _##  
  _##  Licensed under the Apache License, Version 2.0 (the "License");
  _##  you may not use this file except in compliance with the License.
  _##  You may obtain a copy of the License at
  _##  
  _##      http://www.apache.org/licenses/LICENSE-2.0
  _##  
  _##  Unless required by applicable law or agreed to in writing, software
  _##  distributed under the License is distributed on an "AS IS" BASIS,
  _##  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  _##  See the License for the specific language governing permissions and
  _##  limitations under the License.
  _##  
  _##########################################################################*/

package com.j2s.snmp.sample;

import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.*;
import org.snmp4j.agent.cfg.EngineBootsCounterFile;
import org.snmp4j.agent.io.DefaultMOPersistenceProvider;
import org.snmp4j.agent.io.MOInput;
import org.snmp4j.agent.io.MOInputFactory;
import org.snmp4j.agent.io.prop.PropertyMOInput;
import org.snmp4j.agent.mo.*;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.agent.mo.snmp.TimeStamp;
import org.snmp4j.agent.mo.util.VariableProvider;
import org.snmp4j.agent.request.Request;
import org.snmp4j.agent.request.RequestStatus;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.agent.request.SubRequestIterator;
import org.snmp4j.log.ConsoleLogFactory;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TLSTM;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ArgumentParser;
import org.snmp4j.util.SnmpConfigurator;
import org.snmp4j.util.ThreadPool;

import java.io.*;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The SampleAgent uses an {@link AgentConfigManager} instance to create a minimal SNMP agent using the configuration
 * defined by {@code SampleAgentConfig.properties} in this package. That properties file defines the initial content of
 * the registered MIB objects of this agent which may differ from the hard coded defaults.
 * <p>
 * In order to add a new MIB object, call {@code server.register(..)} or replace the {@code Modules.java} file in this
 * package by the {@code Modules.java} generated by AgenPro for your MIB module(s).
 * <p>
 * The agent uses the {@link ConsoleLogFactory} to log messages.
 *
 * @author Frank Fock
 * @version 2.7.3
 */
public class SampleAgent implements VariableProvider {

    static {
        LogFactory.setLogFactory(new ConsoleLogFactory());
        LogFactory.getLogFactory().getRootLogger().setLogLevel(LogLevel.ALL);
    }

    private static final LogAdapter logger = LogFactory.getLogger(SampleAgent.class);

    protected AgentConfigManager agent;
    protected MOServer server;
    private String configFile;
    private File bootCounterFile;

    // supported MIBs
    protected Modules modules;

    protected Properties tableSizeLimits;

    @SuppressWarnings("unchecked")
    public SampleAgent(Map<String, List<?>> args) {
        configFile = (String) (args.get("c")).get(0);
        bootCounterFile = new File((String) (args.get("bc")).get(0));
        List<?> tlsVersions = args.get("tls-version");
        if (tlsVersions != null && (tlsVersions.size() > 0)) {
            System.setProperty(SnmpConfigurator.P_TLS_VERSION, (String) tlsVersions.get(0));
        }

        server = new DefaultMOServer();
        MOServer[] moServers = new MOServer[]{server};
        InputStream configInputStream =
                SampleAgent.class.getResourceAsStream("SampleAgentConfig.properties");
        if (args.containsKey("cfg")) {
            String configFilename = (String) ArgumentParser.getValue(args, "cfg", 0);
            try {
                configInputStream = new FileInputStream(configFilename);
            } catch (FileNotFoundException ex1) {
                logger.error("Config file '" + configFilename + "' not found: " + ex1.getMessage(), ex1);
                throw new RuntimeException(ex1);
            }
        }
        final Properties props = new Properties();
        try {
            props.load(configInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        MOInputFactory configurationFactory = new MOInputFactory() {
            public MOInput createMOInput() {
                return new PropertyMOInput(props, SampleAgent.this);
            }
        };
        InputStream tableSizeLimitsInputStream =
                SampleAgent.class.getResourceAsStream("SampleAgentTableSizeLimits.properties");
        if (args.containsKey("ts")) {
            try {
                tableSizeLimitsInputStream =
                        new FileInputStream((String) ArgumentParser.getValue(args, "ts", 0));
            } catch (FileNotFoundException ex1) {
                ex1.printStackTrace();
            }
        }
        tableSizeLimits = new Properties();
        try {
            tableSizeLimits.load(tableSizeLimitsInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        MessageDispatcher messageDispatcher = new MessageDispatcherImpl();
        // Add TLSTM
        try {
            messageDispatcher.addTransportMapping(new TLSTM(new TlsAddress()));
        } catch (IOException iox) {
            logger.error("Caught exception while adding TLS transport mapping: "+iox.getMessage(), iox);
        }
        addListenAddresses(messageDispatcher, args.get("address"));
        EngineBootsCounterFile engineBootsCounterFile = new EngineBootsCounterFile(bootCounterFile);
        OctetString ownEmngineId = engineBootsCounterFile.getEngineId(new OctetString(MPv3.createLocalEngineID()));
        agent = new AgentConfigManager(ownEmngineId,
                messageDispatcher,
                null,
                moServers,
                ThreadPool.create("SampleAgent", 3),
                configurationFactory,
                new DefaultMOPersistenceProvider(moServers, configFile), engineBootsCounterFile, null);
    }

    protected void addListenAddresses(MessageDispatcher md, List<?> addresses) {
        for (Object addressString : addresses) {
            Address address = GenericAddress.parse(addressString.toString());
            if (address == null) {
                logger.fatal("Could not parse address string '" + addressString + "'");
                return;
            }
            TransportMapping<? extends Address> tm =
                    TransportMappings.getInstance().createTransportMapping(address);
            if (tm != null) {
                md.addTransportMapping(tm);
            } else {
                logger.warn("No transport mapping available for address '" +
                        address + "'.");
            }
        }
    }

    public void run() {
        // Contexts need to be added programmatically
        server.addContext(new OctetString("context1"));
        // initialize agent before registering our own modules
        agent.initialize();
        SNMPv2MIB context1SNMPv2MIB = new SNMPv2MIB(new OctetString(), new OID(), new Integer32(0));
        // switch logging of notifications to log sent notifications instead
        try {
            context1SNMPv2MIB.registerMOs(server, new OctetString("context1"));
        } catch (DuplicateRegistrationException e) {
            throw new RuntimeException(e);
        }
        // switch logging of notifications to log sent notifications instead
        // of logging the original internal notification event:
        //agent.getNotificationLogMIB().setLoggerMode(
        //  NotificationLogMib.Snmp4jNotificationLogModeEnum.sent);
        // this requires sysUpTime to be available.
        // add proxy forwarder
        agent.setupProxyForwarder();

        registerMIBs();
        // apply table size limits
        agent.setTableSizeLimits(tableSizeLimits);
        // register shutdown hook to be able to automatically commit configuration to persistent storage
        agent.registerShutdownHook();
        // now continue agent setup and launch it.
        agent.run();
    }

    /**
     * Get the {@link MOFactory} that creates the various MOs (MIB Objects).
     *
     * @return a {@link DefaultMOFactory} instance by default.
     * @since 1.3.2
     */
    protected MOFactory getFactory() {
        return DefaultMOFactory.getInstance();
    }

    /**
     * Register your own MIB modules in the specified context of the agent. The {@link MOFactory} provided to the {@code
     * Modules} constructor is returned by {@link #getFactory()}.
     */
    protected void registerMIBs() {
        if (modules == null) {
            modules = new Modules(getFactory());
            modules.getSnmp4jDemoMib().getSnmp4jDemoEntry().addMOTableRowListener(
                    new DemoTableRowListener());
            ((TimeStamp) modules.getSnmp4jDemoMib().getSnmp4jDemoEntry().
                    getColumn(Snmp4jDemoMib.idxSnmp4jDemoEntryCol4)).
                    setSysUpTime(agent.getSysUpTime());
        }
        try {
            modules.registerMOs(server, null);
      /* Some alternatives
      // Register a scalar with your OID in your enterprise subtree:
      MOScalar myScalar = new MOScalar(new OID("<scalarOID.0>"),
                                       MOAccessImpl.ACCESS_READ_CREATE,
                                       new OctetString("myText"));
      server.register(myScalar, null);
      // Register a table with a string index and a single integer payload column
      // a row status column to
      DefaultMOTable myTable =
         new DefaultMOTable(new OID("<tableEntryOID>"),
                            new MOTableIndex(new MOTableSubIndex[] {
                                             new MOTableSubIndex(new OID("<indexObjectClassOID>"),
                                             SMIConstants.SYNTAX_OCTET_STRING, 1, 16) },
                                             true),
         new MOMutableColumn[] {
      new MOMutableColumn(1, SMIConstants.SYNTAX_INTEGER32,
                          MOAccessImpl.ACCESS_READ_CREATE,
                          new Integer32(10), true),
      new RowStatus(2)
         });
      server.register(myTable, null);
      */
        } catch (DuplicateRegistrationException drex) {
            logger.error("Duplicate registration: " + drex.getMessage() + "." +
                    " MIB object registration may be incomplete!", drex);
        }
    }


  public Variable getVariable(String name) {
    OID oid;
    OctetString context = null;
    int pos = name.indexOf(':');
    if (pos >= 0) {
      context = new OctetString(name.substring(0, pos));
      oid = new OID(name.substring(pos+1, name.length()));
    }
    else {
      oid = new OID(name);
    }
    final DefaultMOContextScope scope =
        new DefaultMOContextScope(context, oid, true, oid, true);
    MOQuery query = new MOQueryWithSource(scope, false, this);
    ManagedObject mo = server.lookup(query);
    if (mo != null) {
      final VariableBinding vb = new VariableBinding(oid);
      final RequestStatus status = new RequestStatus();
      SubRequest req = new SubRequest() {
        private boolean completed;
        private MOQuery query;

        public boolean hasError() {
          return false;
        }

        public void setErrorStatus(int errorStatus) {
          status.setErrorStatus(errorStatus);
        }

        public int getErrorStatus() {
          return status.getErrorStatus();
        }

        public RequestStatus getStatus() {
          return status;
        }

        public MOScope getScope() {
          return scope;
        }

        public VariableBinding getVariableBinding() {
          return vb;
        }

        public Request getRequest() {
          return null;
        }

        public Object getUndoValue() {
          return null;
        }

        public void setUndoValue(Object undoInformation) {
        }

        public void completed() {
          completed = true;
        }

        public boolean isComplete() {
          return completed;
        }

        public void setTargetMO(ManagedObject managedObject) {
        }

        public ManagedObject getTargetMO() {
          return null;
        }

        public int getIndex() {
          return 0;
        }

        public void setQuery(MOQuery query) {
          this.query = query;
        }

        public MOQuery getQuery() {
          return query;
        }

        public SubRequestIterator<SubRequest> repetitions() {
          return null;
        }

        public void updateNextRepetition() {
        }

        public Object getUserObject() {
          return null;
        }

        public void setUserObject(Object userObject) {
        }

      };
      mo.get(req);
      return vb.getVariable();
    }
    return null;
  }


    class DemoTableRowListener implements MOTableRowListener<Snmp4jDemoMib.Snmp4jDemoEntryRow> {
        public void rowChanged(MOTableRowEvent<Snmp4jDemoMib.Snmp4jDemoEntryRow> event) {
            if ((event.getType() == MOTableRowEvent.CREATE) ||
                    (event.getType() == MOTableRowEvent.UPDATED)) {
                // ignore
                return;
            }
            // update counter
            Counter32 counter = (Counter32)
                    event.getRow().getValue(Snmp4jDemoMib.idxSnmp4jDemoEntryCol3);
            if (counter == null) {
                counter = new Counter32(0);
                ((MOMutableTableRow)
                        event.getRow()).setValue(Snmp4jDemoMib.idxSnmp4jDemoEntryCol3,
                        counter);
            }
            counter.increment();
            // update timestamp
            TimeStamp timestamp = (TimeStamp)
                    event.getTable().getColumn(Snmp4jDemoMib.idxSnmp4jDemoEntryCol4);
            timestamp.update((MOMutableTableRow) event.getRow(),
                    Snmp4jDemoMib.idxSnmp4jDemoEntryCol4);
            // fire notification
            Integer32 type =
                    new Integer32(Snmp4jDemoMib.Snmp4jDemoTableRowModificationEnum.updated);
            switch (event.getType()) {
                case MOTableRowEvent.ADD:
                    type.setValue(Snmp4jDemoMib.Snmp4jDemoTableRowModificationEnum.created);
                    break;
                case MOTableRowEvent.DELETE:
                    type.setValue(Snmp4jDemoMib.Snmp4jDemoTableRowModificationEnum.deleted);
                    break;
            }
            VariableBinding[] payload = new VariableBinding[2];
            OID table = event.getTable().getOID();
            OID updateCount = new OID(table);
            updateCount.append(Snmp4jDemoMib.colSnmp4jDemoEntryCol3);
            updateCount.append(event.getRow().getIndex());

            OID modifyType = new OID(table);
            modifyType.append(Snmp4jDemoMib.colSnmp4jDemoTableRowModification);
            modifyType.append(event.getRow().getIndex());

            payload[0] = new VariableBinding(updateCount, counter);
            payload[1] = new VariableBinding(modifyType, type);
            modules.getSnmp4jDemoMib().snmp4jDemoEvent(
                    agent.getNotificationOriginator(), new OctetString(), payload);
        }
    }

    /**
     * Runs a sample agent with a default configuration defined by {@code SampleAgentConfig.properties}. A sample
     * command line is:
     * <pre>
     * -c SampleAgent.cfg -bc SampleAgent.bc udp:127.0.0.1/4700 tcp:127.0.0.1/4700
     * </pre>
     *
     * @param args
     *         the command line arguments defining at least the listen addresses. The format is {@code
     *         -c[s{=SampleAgent.cfg}] -bc[s{=SampleAgent.bc}] +ts[s] +cfg[s] #address[s&lt;(udp|tcp|tls):.*[/[0-9]+]?&gt;]
     *         ..}. For the format description see {@link ArgumentParser}.
     */
    public static void main(String[] args) {
        ArgumentParser parser =
                new ArgumentParser("-c[s{=SampleAgent.cfg}] -bc[s{=SampleAgent.bc}] +dhks[s] +u[s] " +
                        "+tls-trust-ca[s] +tls-peer-id[s] +tls-local-id[s] +tls-version[s{=TLSv1}<(TLSv1|TLSv1.1|TLSv1.2)>] +dtls-version[s{=TLSv1.2}<(TLSv1.0|TLSv1.2)>]" +
                        "+Djavax.net.ssl.keyStore +Djavax.net.ssl.keyStorePassword " +
                        "+Djavax.net.ssl.trustStore +Djavax.net.ssl.trustStorePassword " +
                        "+ts[s] +cfg[s] +tls-version[s{=TLSv1}<TLSv1[\\.1|\\.2]?[,TLSv1[\\.1|\\.2]?]*>] ",
                        "#address[s<(udp|tcp|tls|dtls):.*[/[0-9]+]?>] ..");
        Map<String, List<?>> commandLineParameters;
        try {
            commandLineParameters = parser.parse(args);
            SampleAgent sampleAgent = new SampleAgent(commandLineParameters);
            // Add all available security protocols (e.g. SHA,MD5,DES,AES,3DES,..)
            SecurityProtocols.getInstance().addDefaultProtocols();
            // configure system group:
            // Set system description:
            // sampleAgent.agent.getSysDescr().setValue("My system description".getBytes());
            // Set system OID (= OID of the AGENT-CAPABILITIES statement describing
            // the implemented MIB objects of this agent:
            // sampleAgent.agent.getSysOID().setValue("1.3.1.6.1.4.1....");
            // Set the system services
            // sampleAgent.agent.getSysServices().setValue(72);
            sampleAgent.run();
      /*
      for (int i=1; i<5; i++) {
        sampleAgent.agent.getAgentNotificationOriginator().notify(
            new OctetString(), SnmpConstants.coldStart,
            new VariableBinding[] {
          new VariableBinding(new OID("1.3.6.1.4.0"), new Integer32(i)),
          new VariableBinding(new OID("1.3.6.1.4.0"),new Counter32(278070606)),
          new VariableBinding(new OID("1.3.6.1.4.0"),new OctetString("Hello world!")),
          new VariableBinding(new OID("1.3.6.1.4.0"),new IpAddress("127.0.0.2")),
          new VariableBinding(new OID("1.3.6.1.4.0"),new Gauge32(867685L))
        });
      }
      */
        } catch (ParseException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }


}

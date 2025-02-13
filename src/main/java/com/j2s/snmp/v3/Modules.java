//package com.j2s.snmp.v3;
//
//import com.j2s.snmp.v3.mib.SampleMib;
//import lombok.extern.slf4j.Slf4j;
//import org.snmp4j.agent.DuplicateRegistrationException;
//import org.snmp4j.agent.MOGroup;
//import org.snmp4j.agent.MOServer;
//import org.snmp4j.agent.mo.MOFactory;
//import org.snmp4j.smi.OctetString;
//
//@Slf4j
//public class Modules implements MOGroup {
//
//	private SampleMib sampleMib;
//
//	private MOFactory factory;
//
////--AgentGen BEGIN=_MEMBERS
////--AgentGen END
//
//	public Modules() {
//		sampleMib = new SampleMib(123456);
////--AgentGen BEGIN=_DEFAULTCONSTRUCTOR
////--AgentGen END
//	}
//
//	public Modules(MOFactory factory) {
//		sampleMib = new SampleMib(123456);
////--AgentGen BEGIN=_CONSTRUCTOR
////--AgentGen END
//	}
//
//	public void registerMOs(MOServer server, OctetString context)
//			throws DuplicateRegistrationException {
//		sampleMib.registerMOs(server, context);
////--AgentGen BEGIN=_registerMOs
////--AgentGen END
//	}
//
//	public void unregisterMOs(MOServer server, OctetString context) {
//		sampleMib.unregisterMOs(server, context);
////--AgentGen BEGIN=_unregisterMOs
////--AgentGen END
//	}
//}
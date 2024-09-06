package com.javashell.audio;

import java.util.EnumSet;
import java.util.HashMap;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackStatus;

public abstract class JackAudioDigestor implements JackProcessCallback {

	private HashMap<String, JackPort> inputPorts, outputPorts;

	private JackClient client;

	public JackAudioDigestor(String clientName, String[] inputs, String[] outputs) {
		if (clientName == null)
			throw new NullPointerException("clientName cannot be null");

		if (inputs == null) {
			inputs = new String[] {};
		}

		if (outputs == null) {
			outputs = new String[] {};
		}

		try {
			Jack jack = Jack.getInstance();
			EnumSet<JackOptions> ops = EnumSet.of(JackOptions.JackNoStartServer);
			EnumSet<JackStatus> status = EnumSet.noneOf(JackStatus.class);

			client = jack.openClient(clientName, ops, status);

			inputPorts = new HashMap<String, JackPort>();
			EnumSet<JackPortFlags> flags = EnumSet.of(JackPortFlags.JackPortIsInput);
			for (int i = 0; i < inputs.length; i++) {
				inputPorts.put(inputs[i], client.registerPort(inputs[i], JackPortType.AUDIO, flags));
			}

			outputPorts = new HashMap<String, JackPort>();
			flags = EnumSet.of(JackPortFlags.JackPortIsOutput);
			for (int i = 0; i < outputs.length; i++) {
				outputPorts.put(outputs[i], client.registerPort(outputs[i], JackPortType.AUDIO, flags));
			}

			client.setProcessCallback(this);
			client.activate();
		} catch (JackException e) {
			e.printStackTrace();
		}
	}

	public final HashMap<String, JackPort> getInputs() {
		return inputPorts;
	}

	public final HashMap<String, JackPort> getOutputs() {
		return outputPorts;
	}

	public final int getSampleRate() {
		try {
			return client.getSampleRate();
		} catch (JackException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public final int getBufferSize() {
		try {
			return client.getBufferSize();
		} catch (JackException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public final JackClient getRawJackClient() {
		return client;
	}

}

package com.crowdin.cli.commands;

import com.crowdin.cli.client.Client;
import com.crowdin.cli.commands.Outputter;
import com.crowdin.cli.properties.PropertiesBean;

public interface ClientAction {

    void act(Outputter out, PropertiesBean pb, Client client);
}

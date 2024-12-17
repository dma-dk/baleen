package dk.dma.baleen.connector.secom.controllers;

import dk.dma.baleen.connector.secom.spi.AuthenticatedMcpNode;

public record SecomNode(String mrn) implements AuthenticatedMcpNode {

}
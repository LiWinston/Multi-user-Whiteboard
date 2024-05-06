package Service;

import WBSYS.WhiteBoard;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.WhiteBoardClientServiceGrpc;

import java.util.logging.Logger;

public class WhiteBoardClientImpl extends WhiteBoardClientServiceGrpc.WhiteBoardClientServiceImplBase {
    public WhiteBoard wb;
    public Logger logger;

    WhiteBoardClientImpl(WhiteBoard wb, Logger logger) {
        super();
        this.wb = wb;
        this.logger = logger;
    }


}
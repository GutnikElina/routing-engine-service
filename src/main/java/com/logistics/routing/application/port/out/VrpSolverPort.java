package com.logistics.routing.application.port.out;

import com.logistics.routing.domain.vrp.model.VrpProblem;
import com.logistics.routing.domain.vrp.model.VrpSolution;

public interface VrpSolverPort {

    VrpSolution solve(VrpProblem problem);
}

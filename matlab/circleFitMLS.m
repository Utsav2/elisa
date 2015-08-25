function [ c, R ] = circleFitMLS( x, y )
    n = length(x);
    sumX = sum(x);
    sumX2 = sum(x.*x);
    sumX3 = sum(x.*x.*x);
    sumY = sum(y);
    sumY2 = sum(y.*y);
    sumY3 = sum(y.*y.*y);
    sumXY = sum(x.*y);
    sumXY2 = sum(x.*y.*y);
    sumX2Y = sum(x.*x.*y);
    A = n*sumX2 - sumX*sumX;
    B = n*sumXY - sumX*sumY;
    C = n*sumY2 - sumY*sumY;
    D = 0.5*(n*sumXY2 - sumX*sumY2 + n*sumX3 - sumX*sumX2);
    E = 0.5*(n*sumX2Y - sumY*sumX2 + n*sumY3 - sumY*sumY2);
    c = [(D*C - B*E)/(A*C - B*B); (A*E - B*D)/(A*C - B*B)];
    R = sum(sqrt((x - c(1)).*(x - c(1)) + (y - c(2)).*(y - c(2))))/n;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
end


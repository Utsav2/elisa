function [ R ] = calculateRadius( x, y, c )
    n = length(x);
    R = sum(sqrt((x - c(1)).*(x - c(1)) + (y - c(2)).*(y - c(2))))/n;
end


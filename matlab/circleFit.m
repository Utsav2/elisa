function [ c, R ] = circleFit( x, y )
    line = zeros(2, 2);
    m = zeros(2, 2);
    a = zeros(2, 1);
    b = zeros(2, 1);
    for i=1:2
        line(:, i) = [x(i) 1; x(i+1) 1]\[y(i); y(i+1)];
        m(:, i) = [(x(i) + x(i+1))/2; (y(i) + y(i+1))/2];
        a(i) = -1/line(1, i);
        b(i) = m(2, i) - a(i)*m(1, i);
    end
    c = [-a(1) 1; -a(2) 1]\[b(1); b(2)];
    R = sqrt((x(1) - c(1))*(x(1) - c(1)) + (y(1) - c(2))*(y(1) - c(2)));
end


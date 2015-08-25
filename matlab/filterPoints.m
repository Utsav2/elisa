function [ x1, y1 ] = filterPoints( x, y, n )
    index = find(x > (mean(x) + n*std(x)) | x < (mean(x) - n*std(x)));
    x1 = x;
    y1 = y;
    x1(index) = [];
    y1(index) = [];
end


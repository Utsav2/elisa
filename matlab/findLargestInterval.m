function [ x1, x2 ] = findLargestInterval( x, thresh )
    diff = zeros(length(x) - 1, 1);
    for i=1:length(diff)
        diff(i) = x(i+1) - x(i);
    end
    x1 = 1;
    x2 = 1;
    range = 1;
    tempRange = 1;
    tempX1 = 1;
    isPairing = false;
    i = 1;
    for i=1:length(diff)
        if (abs(diff(i)) > thresh && ~isPairing)
            continue;
        elseif (abs(diff(i)) > thresh && isPairing)
            if (tempRange > range)
                x1 = tempX1;
                x2 = i - 1;
                range = tempRange;
            end
            isPairing = false;
        elseif (abs(diff(i)) <= thresh && ~isPairing)
            tempX1 = i;
            tempRange = 1;
            isPairing = true;
        else
            tempRange = tempRange + 1;
        end
    end
    if (isPairing && tempRange > range)
        x1 = tempX1;
        x2 = length(diff);
    end
    x2 = x2 + 1;
end


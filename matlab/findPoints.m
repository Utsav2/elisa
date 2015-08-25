function [ px, py ] = findPoints( I, channel, range, thresh, climbMax)
    px = (range(1):range(2))';
    py = zeros(length(px), 1);
    for i=1:length(px)
        col = I(:, px(i), :);
%         figure;
%     plot(col(:, 1), 'r');
%     hold on;
%     plot(col(:, 2), 'g');
%     plot(col(:, 3), 'b');
        [maxVal, iMax] = max(col(:, channel));
        threshVal = double(thresh * maxVal);
%         j = 1;
%         if (climbMax)
%             while (col(j, channel) < maxVal)
%                 j = j + 1;
%             end
%             while (col(j, channel) >= threshVal)
%                 j = j + 1;
%             end
%         else 
%             while (col(j, channel) < threshVal)
%                 j = j + 1;
%             end
%         end
        j = iMax;
        while (col(j, channel) >= threshVal) 
            j = j + 1;
        end
        y1 = j - 1;
        val1 = double(col(y1, channel));
        y2 = j;
        val2 = double(col(y2, channel));
        a = [y1 1; y2 1] \ [val1; val2];
        py(i) = (threshVal - a(2)) / a(1);
    end
end


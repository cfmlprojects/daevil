@param daevil.menu.BatchFileBuilder builder

:prompt_confirm
   set /p answer=%~1
   if /i "%answer:~,1%" EQU "Y" exit /B 1
   if /i "%answer:~,1%" EQU "N" exit /B 0
   echo Please type Y for Yes or N for No
   goto prompt_confirm

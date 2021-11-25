import csv


INSTANCECLUSTERS = {}
# extract existing clusters:
with open('instance-clusters.csv', newline='') as csvfile:
    srcreader = csv.reader(csvfile, delimiter=',')
    for row in srcreader:
        INSTANCECLUSTERS["M"+row[0]] = row[1]


with open('clusters-manual.csv', newline='') as csvfile:
    srcreader = csv.reader(csvfile, delimiter=',')
    for row in srcreader:
        if row[0] in INSTANCECLUSTERS:
        	print(INSTANCECLUSTERS[row[0]]+","+row[1])
